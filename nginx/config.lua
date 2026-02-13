local _M = {}
_M.AUTHORIZATION_ENDPOINT = "http://localhost:8008/realms/greeter/protocol/openid-connect/auth"
_M.TOKEN_ENDPOINT = "http://keycloak:8000/realms/greeter/protocol/openid-connect/token"
_M.CLIENT_ID = "bff"
_M.CLIENT_SECRET = "2Z1LoQEM9oaF5mBxe5y9HeEcXKSmnU8s"
_M.REDIRECT_URI = "http://localhost:8008/callback"
_M.SCOPE = "openid profile email"
return _M
