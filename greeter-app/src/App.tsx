import { useState, useEffect, useRef } from 'react'

import './App.css'

// Use relative URLs - frontend nginx proxies to BFF
const GATEWAY_URL = ''
const WS_URL = '/ws/greeter'

interface Greeting {
  greeting: string
}

interface UserInfo {
  authenticated: boolean
  sub?: string
  email?: string
  name?: string
  username?: string
}

function App() {
  const [data, setData] = useState<Greeting | null>(null)
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [wsConnected, setWsConnected] = useState(false)
  const stompClientRef = useRef<any>(null)
  const checkingAuthRef = useRef(false)

  useEffect(() => {
    // Don't check auth if we're on login/callback/logout pages
    // These pages are handled by the BFF and will redirect appropriately
    const currentPath = window.location.pathname
    if (currentPath.includes('/login') || 
        currentPath.includes('/callback') || 
        currentPath.includes('/logout')) {
      console.log('⏸️ Skipping auth check on', currentPath, '- BFF will handle redirect')
      return
    }
    
    // After callback, wait a bit for cookie to be available before checking auth
    // This prevents the loop where cookie isn't available yet
    const delay = currentPath === '/' ? 500 : 0
    
    setTimeout(() => {
      // Only check auth once on mount for other pages
      if (!checkingAuthRef.current) {
        checkAuth()
      }
    }, delay)
    
    // Cleanup: reset ref on unmount (in case component unmounts before auth check completes)
    return () => {
      // Don't reset here - let checkAuth handle it when it completes
    }
  }, [])

  useEffect(() => {
    // Connect to WebSocket ONLY after successful /api/user call
    // This ensures the session cookie is set and authentication is confirmed
    if (userInfo?.authenticated) {
      console.log('✅ User authenticated, connecting WebSocket...')
      // Small delay to ensure cookie is available
      const timer = setTimeout(() => {
        connectWebSocket()
      }, 100)
      
      return () => {
        clearTimeout(timer)
        if (stompClientRef.current) {
          try {
            stompClientRef.current.deactivate()
          } catch (err) {
            console.error('Error deactivating WebSocket:', err)
          }
        }
      }
    } else {
      // Disconnect WebSocket if user is not authenticated
      if (stompClientRef.current) {
        console.log('❌ User not authenticated, deactivating WebSocket...')
        try {
          stompClientRef.current.deactivate()
          stompClientRef.current = null
          setWsConnected(false)
        } catch (err) {
          console.error('Error deactivating WebSocket:', err)
        }
      }
    }
  }, [userInfo?.authenticated])

  const connectWebSocket = () => {
    console.log('🔌 Attempting to connect WebSocket to:', WS_URL)
    console.log('👤 User authenticated:', userInfo?.authenticated)

    if (stompClientRef.current?.active) {
      console.log('⚠️ WebSocket already active')
      return
    }

    if (!userInfo?.authenticated) {
      console.log('❌ Cannot connect WebSocket: user not authenticated')
      return
    }

    import('sockjs-client').then((SockJSModule: any) => {
      import('@stomp/stompjs').then((Stomp) => {
        const SockJS = SockJSModule.default || SockJSModule

        const stompClient = new Stomp.Client({
        
          webSocketFactory: () =>
            new SockJS(WS_URL, null, {
              transports: ['websocket', 'xhr-streaming', 'xhr-polling']
            }),
          reconnectDelay: 5000,
          debug: (str: string) => {
            console.log('STOMP:', str)
          },
          onConnect: () => {
            setWsConnected(true)
            console.log('✅ STOMP connected successfully')

            stompClient.subscribe('/ws/greeting', (message) => {
              try {
                const greeting: Greeting = JSON.parse(message.body)
                console.log('📨 Received greeting:', greeting)
                setData(greeting)
              } catch (err) {
                console.error('Error parsing greeting message:', err)
              }
            })
            console.log('✅ Subscribed to /ws/greeting topic')
          },
          onStompError: (frame) => {
            console.error('❌ STOMP error:', frame.headers['message'], frame.body)
            setWsConnected(false)
          },
          onWebSocketClose: () => {
            console.log('❌ WebSocket closed – will reconnect automatically')
            setWsConnected(false)
          }
        })

        stompClient.activate()
        stompClientRef.current = stompClient
      }).catch((err) => {
        console.error('❌ Failed to load STOMP library:', err)
        setWsConnected(false)
      })
    }).catch((err) => {
      console.error('❌ Failed to load SockJS library:', err)
      setWsConnected(false)
    })
  }

  const checkAuth = async () => {
    // Prevent multiple simultaneous auth checks
    if (checkingAuthRef.current) {
      console.log('⏸️ Auth check already in progress, skipping...')
      return
    }
    
    checkingAuthRef.current = true
    console.log('🔍 Checking authentication...')
    console.log('📍 Current URL:', window.location.href)
    console.log('🍪 Cookies:', document.cookie)
    
    try {
      const response = await fetch(`${GATEWAY_URL}/api/user`, {
        credentials: 'include',
        redirect: 'manual' // Don't follow redirects automatically
      })
      
      console.log('📡 Response status:', response.status)
      console.log('📡 Response headers:', Object.fromEntries(response.headers.entries()))
      
      if (response.ok) {
        const user = await response.json()
        console.log('✅ Authenticated as:', user)
        console.log('✅ Setting userInfo, WebSocket will connect automatically...')
        setUserInfo(user)
        checkingAuthRef.current = false
        // WebSocket will connect automatically via useEffect when userInfo.authenticated is true
      } else if (response.status === 401 || response.status === 0) {
        console.log('❌ Not authenticated (401)')
        // Not authenticated - redirect to BFF /login endpoint
        // Only redirect if not already on login/callback/logout pages
        const currentPath = window.location.pathname
        if (!currentPath.includes('/login') && 
            !currentPath.includes('/callback') && 
            !currentPath.includes('/logout')) {
          console.log('🔄 Redirecting to BFF login endpoint...')
          window.location.href = `${GATEWAY_URL}/login`
        } else {
          console.log('⏸️ Already on', currentPath, '- BFF will handle redirect')
        }
        checkingAuthRef.current = false
        return
      } else {
        console.error('⚠️ Unexpected response status:', response.status)
        checkingAuthRef.current = false
      }
    } catch (err) {
      console.error('❌ Auth check failed:', err)
      // Redirect to BFF /login endpoint on error
      // Only redirect if not already on login/callback/logout pages
      const currentPath = window.location.pathname
      if (!currentPath.includes('/login') && 
          !currentPath.includes('/callback') && 
          !currentPath.includes('/logout')) {
        console.log('🔄 Redirecting to BFF login endpoint due to error...')
        window.location.href = `${GATEWAY_URL}/login`
      } else {
        console.log('⏸️ Already on', currentPath, '- BFF will handle redirect')
      }
      checkingAuthRef.current = false
    }
  }

  const handleGreet = async () => {
    try {
      setError(null)
      const response = await fetch(`${GATEWAY_URL}/api/greeter/greet`, {
        credentials: 'include'
      })
      if (!response.ok) {
        if (response.status === 401) {
          setError('Session expired. Please login again.')
          setUserInfo(null)
          // Redirect naar login bij expired session
          window.location.href = `${GATEWAY_URL}/login`
          return
        }
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const greeting = await response.json()
      console.log(greeting)
      setData(greeting)
    } catch (err) {
      console.error('Failed to fetch greeting:', err)
      setError('Failed to fetch greeting. Please try again.')
    }
  }

  const handleLogout = () => {
    if (stompClientRef.current) {
      try {
        stompClientRef.current.deactivate()
      } catch (err) {
        console.error('Error deactivating WebSocket:', err)
      }
    }
    window.location.href = `${GATEWAY_URL}/logout`
  }

  // Als userInfo nog niet geladen is of niet geauthenticeerd, toon niets (redirect gebeurt al)
  if (!userInfo?.authenticated) {
    return null
  }

  return (
    <div>
      <h1>Greeter App</h1>
      <div>
        <div>
          <p>Logged in as: {userInfo.name || userInfo.username || userInfo.email}</p>
          <p>WebSocket: {wsConnected ? '✅ Connected' : '❌ Disconnected'}</p>
        </div>
        {error && (<div style={{ color: 'red' }}>{error}</div>)}
        {data && (
          <div>
            <h2>Current Greeting:</h2>
            <p style={{ fontSize: '1.2em', fontWeight: 'bold' }}>{data.greeting}</p>
          </div>
        )}
        <div>
          <button onClick={handleGreet}>Greet me (manual)</button>
          <button onClick={handleLogout}>Logout</button>
        </div>
      </div>
    </div>
  )
}

export default App
