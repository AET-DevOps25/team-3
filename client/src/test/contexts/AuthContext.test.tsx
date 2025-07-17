import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider, useAuth } from '../../contexts/AuthContext'
import { server } from '../mocks/server'
import { http, HttpResponse } from 'msw'

// Test component to access auth context
const TestComponent = () => {
  const { user, login, logout, register, isLoading } = useAuth()
  
  return (
    <div>
      <div data-testid="loading">{isLoading ? 'Loading' : 'Not Loading'}</div>
      <div data-testid="user">{user ? user.username : 'No User'}</div>
      <button 
        onClick={() => login({ username: 'testuser', password: 'password' })}
        data-testid="login-btn"
      >
        Login
      </button>
      <button 
        onClick={() => register({
          username: 'newuser',
          email: 'new@example.com',
          password: 'password',
          firstName: 'New',
          lastName: 'User'
        })}
        data-testid="register-btn"
      >
        Register
      </button>
      <button 
        onClick={logout}
        data-testid="logout-btn"
      >
        Logout
      </button>
    </div>
  )
}

// Test wrapper component
const TestWrapper = ({ children }: { children: React.ReactNode }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          {children}
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('should initialize with no user', () => {
    render(<TestComponent />, { wrapper: TestWrapper })
    
    expect(screen.getByTestId('user')).toHaveTextContent('No User')
    expect(screen.getByTestId('loading')).toHaveTextContent('Not Loading')
  })

  it('should handle successful login', async () => {
    const user = userEvent.setup()
    render(<TestComponent />, { wrapper: TestWrapper })
    
    const loginBtn = screen.getByTestId('login-btn')
    
    await act(async () => {
      await user.click(loginBtn)
    })
    
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('testuser')
    })
    
    // Check that a JWT token was set
    expect(localStorage.setItem).toHaveBeenCalledWith('auth_token', 'mock-jwt-token')
  })

  it('should handle login failure', async () => {
    // Override the default login handler to return an error
    server.use(
      http.post('http://localhost:3000/api/auth/login', () => {
        return HttpResponse.json(
          { error: 'Invalid credentials' },
          { status: 401 }
        )
      })
    )
    
    const user = userEvent.setup()
    render(<TestComponent />, { wrapper: TestWrapper })
    
    const loginBtn = screen.getByTestId('login-btn')

    await act(async () => {
      await user.click(loginBtn)
    });
    
    // The login should fail, but the component handles the error
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('No User')
    })
    
    expect(screen.getByTestId('user')).toHaveTextContent('No User')
  })

  it('should handle successful registration', async () => {
    const user = userEvent.setup()
    render(<TestComponent />, { wrapper: TestWrapper })
    
    const registerBtn = screen.getByTestId('register-btn')
    
    await act(async () => {
      await user.click(registerBtn)
    })
    
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('newuser')
    })
    
    // Check that a JWT token was set
    expect(localStorage.setItem).toHaveBeenCalledWith('auth_token', 'mock-jwt-token')
  })

  it('should handle registration failure for existing user', async () => {
    // Override the default register handler to return an error
    server.use(
      http.post('http://localhost:3000/api/auth/register', () => {
        return HttpResponse.json(
          { error: 'Username already exists' },
          { status: 409 }
        )
      })
    )
    
    const user = userEvent.setup()
    render(<TestComponent />, { wrapper: TestWrapper })
    
    const registerBtn = screen.getByTestId('register-btn')

    await act(async () => {
      await user.click(registerBtn)
    });
    
    // The registration should fail, but the component handles the error
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('No User')
    })
    
    expect(screen.getByTestId('user')).toHaveTextContent('No User')
  })

  it('should handle logout', async () => {
    const user = userEvent.setup()
    render(<TestComponent />, { wrapper: TestWrapper })
    
    // First login
    const loginBtn = screen.getByTestId('login-btn')
    await act(async () => {
      await user.click(loginBtn)
    })
    
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('testuser')
    })
    
    // Then logout
    const logoutBtn = screen.getByTestId('logout-btn')
    await act(async () => {
      await user.click(logoutBtn)
    })
    
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('No User')
    })
    
    expect(localStorage.removeItem).toHaveBeenCalledWith('auth_token')
  })

  it('should restore user from stored token on initialization', async () => {
    // Create a valid JWT-like token for testing
    const createMockJWTToken = (username: string = 'testuser') => {
      const header = { alg: 'HS256', typ: 'JWT' }
      const payload = {
        sub: username,
        exp: Math.floor(Date.now() / 1000) + 3600, // 1 hour from now
        iat: Math.floor(Date.now() / 1000),
        username: username
      }
      
      const encodedHeader = btoa(JSON.stringify(header))
      const encodedPayload = btoa(JSON.stringify(payload))
      const signature = 'mock-signature'
      
      return `${encodedHeader}.${encodedPayload}.${signature}`
    }
    
    // Mock existing token in localStorage
    localStorage.setItem('auth_token', createMockJWTToken())
    
    render(<TestComponent />, { wrapper: TestWrapper })
    
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('testuser')
    })
  })

  it('should handle invalid stored token', async () => {
    // Mock existing token in localStorage
    localStorage.setItem('auth_token', 'invalid-token')
    
    // Override the me endpoint to return 401
    server.use(
      http.get('http://localhost:3000/api/auth/me', () => {
        return HttpResponse.json(
          { error: 'Unauthorized' },
          { status: 401 }
        )
      })
    )
    
    render(<TestComponent />, { wrapper: TestWrapper })
    
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('No User')
    })
    
    expect(localStorage.removeItem).toHaveBeenCalledWith('auth_token')
  })

  it('should show loading state during authentication', async () => {
    const user = userEvent.setup()
    render(<TestComponent />, { wrapper: TestWrapper })
    
    const loginBtn = screen.getByTestId('login-btn')
    
    // Mock a delayed response
    server.use(
      http.post('http://localhost:3000/api/auth/login', async () => {
        await new Promise(resolve => setTimeout(resolve, 100))
        return HttpResponse.json({
          token: 'mock-jwt-token',
          user: { id: 1, username: 'testuser', email: 'test@example.com' },
        })
      })
    )
    
    await act(async () => {
      await user.click(loginBtn)
    })
    
    // Should show loading immediately
    expect(screen.getByTestId('loading')).toHaveTextContent('Loading')
    
    // Wait for loading to complete
    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('Not Loading')
    })
  })

  it('should handle network errors gracefully', async () => {
    // Mock network error
    server.use(
      http.post('http://localhost:3000/api/auth/login', () => {
        return HttpResponse.error()
      })
    )
    
    const user = userEvent.setup()
    render(<TestComponent />, { wrapper: TestWrapper })
    
    const loginBtn = screen.getByTestId('login-btn')

    await act(async () => {
      await user.click(loginBtn)
    });
    
    // The network error should be handled gracefully
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('No User')
    })
  })

  it('should handle concurrent login attempts', async () => {
    const user = userEvent.setup()
    render(<TestComponent />, { wrapper: TestWrapper })
    
    const loginBtn = screen.getByTestId('login-btn')
    
    // Trigger multiple login attempts
    await act(async () => {
      const promise1 = user.click(loginBtn)
      const promise2 = user.click(loginBtn)
      await Promise.all([promise1, promise2])
    })
    
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('testuser')
    })
  })
})
