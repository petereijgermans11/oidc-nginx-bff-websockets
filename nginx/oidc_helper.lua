-- Custom OIDC helper for OpenResty with proper port forwarding support
-- This fixes the issue where lua-resty-openidc strips port numbers from redirect URLs

local http = require "resty.http"
local cjson = require "cjson"

local _M = {}

-- Generate random state and nonce
local function generate_random_string(length)
    local charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    local random_string = {}
    local charset_length = #charset
    
    for i = 1, length do
        local rand_index = math.random(1, length)
        table.insert(random_string, charset:sub(rand_index, rand_index))
    end
    
    return table.concat(random_string)
end

-- Build authorization URL with proper port handling
function _M.build_authorization_url(opts)
    local state = generate_random_string(32)
    local nonce = generate_random_string(32)
    
    -- Store state and nonce in session
    ngx.ctx.oidc_state = state
    ngx.ctx.oidc_nonce = nonce
    
    local params = {
        client_id = opts.client_id,
        redirect_uri = opts.redirect_uri,
        response_type = "code",
        scope = opts.scope or "openid profile email",
        state = state,
        nonce = nonce
    }
    
    local query_string = {}
    for k, v in pairs(params) do
        table.insert(query_string, string.format("%s=%s", 
            ngx.escape_uri(k), 
            ngx.escape_uri(v)))
    end
    
    return opts.authorization_endpoint .. "?" .. table.concat(query_string, "&")
end

-- Exchange authorization code for tokens
function _M.exchange_code_for_tokens(opts, code)
    local httpc = http.new()
    httpc:set_timeout(10000)
    
    local body = {
        grant_type = "authorization_code",
        code = code,
        redirect_uri = opts.redirect_uri,
        client_id = opts.client_id,
        client_secret = opts.client_secret
    }
    
    local body_string = {}
    for k, v in pairs(body) do
        table.insert(body_string, string.format("%s=%s", 
            ngx.escape_uri(k), 
            ngx.escape_uri(v)))
    end
    
    local res, err = httpc:request_uri(opts.token_endpoint, {
        method = "POST",
        body = table.concat(body_string, "&"),
        headers = {
            ["Content-Type"] = "application/x-www-form-urlencoded"
        }
    })
    
    if not res then
        return nil, "Failed to connect to token endpoint: " .. (err or "unknown error")
    end
    
    if res.status ~= 200 then
        return nil, "Token endpoint returned status " .. res.status .. ": " .. (res.body or "")
    end
    
    local tokens = cjson.decode(res.body)
    return tokens, nil
end

-- Session management with server-side storage (fixes cookie size limit)
local sessions = ngx.shared.sessions

-- Generate session ID
local function generate_session_id()
    local random_string = generate_random_string(32)
    return ngx.md5(random_string .. tostring(ngx.now()) .. tostring(ngx.worker.pid()))
end

function _M.get_session()
    local session_id = ngx.var.cookie_session
    if not session_id then
        return nil
    end
    
    -- Retrieve session data from shared dict
    local session_json = sessions:get(session_id)
    if not session_json then
        return nil
    end
    
    local ok, session = pcall(cjson.decode, session_json)
    if not ok then
        return nil
    end
    
    return session
end

function _M.set_session(data, max_age)
    -- Generate session ID
    local session_id = generate_session_id()
    
    -- Store session data server-side in shared dict
    local session_json = cjson.encode(data)
    local ttl = max_age or 3600
    local success, err = sessions:set(session_id, session_json, ttl)
    
    if not success then
        ngx.log(ngx.ERR, "Failed to store session: ", err or "unknown")
        return nil
    end
    
    -- Only store small session ID in cookie
    local cookie_parts = {
        "session=" .. session_id,
        "Path=/",
        "HttpOnly",
        "SameSite=Lax"
    }
    
    if max_age then
        table.insert(cookie_parts, "Max-Age=" .. max_age)
    end
    
    return table.concat(cookie_parts, "; ")
end

function _M.update_session(data)
    local session_id = ngx.var.cookie_session
    if not session_id then
        return false, "no session cookie"
    end

    local session_json = cjson.encode(data)
    local success, err = sessions:set(session_id, session_json, 3600)
    if not success then
        ngx.log(ngx.ERR, "Failed to update session: ", err or "unknown")
        return false, err
    end

    return true
end

function _M.refresh_access_token(opts)
    local session = _M.get_session()
    if not session or not session.refresh_token then
        return nil, "no session or refresh token"
    end

    -- Decode access token payload to check exp claim
    local parts = {}
    for part in string.gmatch(session.access_token, "[^%.]+") do
        table.insert(parts, part)
    end

    if #parts >= 2 then
        local payload_json = ngx.decode_base64(parts[2])
        if payload_json then
            local ok, payload = pcall(cjson.decode, payload_json)
            if ok and payload.exp then
                if payload.exp > ngx.time() + 5 then
                    return session
                end
            end
        end
    end

    ngx.log(ngx.ERR, "Access token expired or unreadable, refreshing via refresh_token ...")

    local httpc = http.new()
    httpc:set_timeout(10000)

    local body = {
        grant_type    = "refresh_token",
        refresh_token = session.refresh_token,
        client_id     = opts.client_id,
        client_secret = opts.client_secret
    }

    local body_string = {}
    for k, v in pairs(body) do
        table.insert(body_string, string.format("%s=%s", ngx.escape_uri(k), ngx.escape_uri(v)))
    end

    local headers = { ["Content-Type"] = "application/x-www-form-urlencoded" }
    if opts.token_endpoint_host then
        headers["Host"] = opts.token_endpoint_host
    end

    local res, err = httpc:request_uri(opts.token_endpoint, {
        method  = "POST",
        body    = table.concat(body_string, "&"),
        headers = headers
    })

    if not res then
        return nil, "Failed to connect to token endpoint: " .. (err or "unknown")
    end

    if res.status ~= 200 then
        ngx.log(ngx.ERR, "Token refresh failed (", res.status, "): ", res.body or "")
        return nil, "Token refresh failed with status " .. res.status
    end

    local tokens = cjson.decode(res.body)

    session.access_token = tokens.access_token
    if tokens.refresh_token then
        session.refresh_token = tokens.refresh_token
    end
    if tokens.id_token then
        session.id_token = tokens.id_token
    end

    local ok, uerr = _M.update_session(session)
    if not ok then
        ngx.log(ngx.ERR, "Failed to persist refreshed session: ", uerr)
    end

    ngx.log(ngx.ERR, "Access token refreshed successfully")
    return session
end

function _M.clear_session()
    -- Delete session from shared dict if it exists
    local session_id = ngx.var.cookie_session
    if session_id then
        sessions:delete(session_id)
    end
    return "session=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0"
end

return _M
