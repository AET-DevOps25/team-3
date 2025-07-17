import { http, HttpResponse } from 'msw'
import { UserResponse, DocumentStatus } from '../../lib/api'

// Helper function to create a valid JWT-like token for testing
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

// Helper function to validate test tokens
const isValidTestToken = (token: string): boolean => {
  return token === 'mock-jwt-token' || 
         token === 'test-token' || 
         token.startsWith('test-') || 
         token.startsWith('mock-') ||
         token.startsWith('ey') // JWT tokens start with 'ey' when base64 encoded
}

// Helper function to check authorization
const checkAuth = (request: Request) => {
  const authHeader = request.headers.get('Authorization')
  
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return { authorized: false, error: HttpResponse.json({ error: 'Unauthorized' }, { status: 401 }) }
  }
  
  const token = authHeader.split(' ')[1]
  
  if (!isValidTestToken(token)) {
    return { authorized: false, error: HttpResponse.json({ error: 'Invalid token' }, { status: 401 }) }
  }
  
  return { authorized: true, error: null }
}

// Mock API responses
export const mockUser: UserResponse = {
  id: '1',
  username: 'testuser',
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  fullName: 'Test User',
  role: 'USER',
  isActive: true,
  createdAt: new Date().toISOString(),
  lastLoginAt: null,
}

export const mockDocument = {
  id: 1,
  filename: 'test-document.pdf',
  status: 'PROCESSED' as DocumentStatus,
  uploadedAt: new Date().toISOString(),
  processedAt: new Date().toISOString(),
  size: 1024,
  contentType: 'application/pdf',
  userId: '1',
}

export const mockDocuments = [
  mockDocument,
  {
    ...mockDocument,
    id: 2,
    filename: 'test-document-2.pdf',
    status: 'PROCESSING' as DocumentStatus,
    processedAt: undefined,
  },
]

export const handlers = [
  // Auth endpoints
  http.post('http://localhost:3000/api/auth/login', async ({ request }) => {
    const body = await request.json() as { username: string; password: string }
    
    if (body.username === 'testuser' && body.password === 'password') {
      return HttpResponse.json({
        token: 'mock-jwt-token',
        refreshToken: 'mock-refresh-token',
        expiresIn: 3600,
        user: mockUser,
      })
    }
    
    return HttpResponse.json(
      { error: 'Invalid credentials' },
      { status: 401 }
    )
  }),

  http.post('http://localhost:3000/api/auth/register', async ({ request }) => {
    const body = await request.json() as { username: string; email: string; password: string }
    
    if (body.username === 'existinguser') {
      return HttpResponse.json(
        { error: 'Username already exists' },
        { status: 409 }
      )
    }
    
    return HttpResponse.json({
      token: 'mock-jwt-token',
      refreshToken: 'mock-refresh-token',
      expiresIn: 3600,
      user: {
        ...mockUser,
        username: body.username,
        email: body.email,
      },
    })
  }),

  http.post('http://localhost:3000/api/auth/logout', () => {
    return HttpResponse.json({ message: 'Logged out successfully' })
  }),

  http.get('http://localhost:3000/api/auth/me', ({ request }) => {
    const auth = checkAuth(request)
    if (!auth.authorized) {
      return auth.error
    }
    
    return HttpResponse.json(mockUser)
  }),

  // Document endpoints
  http.get('http://localhost:3000/api/documents', ({ request }) => {
    const auth = checkAuth(request)
    if (!auth.authorized) {
      return auth.error
    }
    
    return HttpResponse.json({ documents: mockDocuments })
  }),

  http.post('http://localhost:3000/api/documents/upload', async ({ request }) => {
    const auth = checkAuth(request)
    if (!auth.authorized) {
      return auth.error
    }

    const formData = await request.formData()
    const files = formData.getAll('files') as File[]

    if (!files || files.length === 0) {
      return HttpResponse.json(
        { error: 'No files provided' },
        { status: 400 }
      )
    }

    const file = files[0]
    const fileType = formData.get('type') as string

    if (file.size > 10 * 1024 * 1024) {
      return HttpResponse.json(
        { error: 'File too large' },
        { status: 413 }
      )
    }

    const allowedTypes = ['application/pdf', 'text/plain', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    if (!allowedTypes.includes(fileType)) {
      return HttpResponse.json(
        { error: `Unsupported file type: ${fileType}` },
        { status: 415 }
      );
    }

    return HttpResponse.json({
      documentIds: ['mock-document-id'],
      status: 'UPLOADED',
    })
  }),

  http.get('http://localhost:3000/api/documents/:id/content', ({ params, request }) => {
    const auth = checkAuth(request)
    if (!auth.authorized) {
      return auth.error
    }
    
    const id = parseInt(params.id as string)
    const document = mockDocuments.find(doc => doc.id === id)
    
    if (!document) {
      return HttpResponse.json(
        { error: 'Document not found' },
        { status: 404 }
      )
    }
    
    return HttpResponse.json({
      ...document,
      content: `This is the content of ${document.filename}`,
      summary: `## Document Summary\n\nThis is a mock summary of ${document.filename}.\n\n### Key Points:\n- Point 1\n- Point 2\n- Point 3\n\n### Conclusion\nThe document covers important topics.`,
    })
  }),

  http.delete('http://localhost:3000/api/documents/:id', ({ params, request }) => {
    const auth = checkAuth(request)
    if (!auth.authorized) {
      return auth.error
    }
    
    const id = parseInt(params.id as string)
    const document = mockDocuments.find(doc => doc.id === id)
    
    if (!document) {
      return HttpResponse.json(
        { error: 'Document not found' },
        { status: 404 }
      )
    }
    
    return HttpResponse.json({ message: 'Document deleted successfully' })
  }),

  // GenAI endpoints
  http.post('http://localhost:3000/api/genai/chat/sessions', async ({ request }) => {
    const auth = checkAuth(request)
    if (!auth.authorized) {
      return auth.error
    }
    
    const body = await request.json() as { documentIds: string[] }
    
    return HttpResponse.json({
      sessionId: 'mock-session-id',
      documentIds: body.documentIds,
      createdAt: new Date().toISOString(),
    })
  }),

  http.post('http://localhost:3000/api/genai/chat/sessions/:sessionId/messages', async ({ request, params }) => {
    const auth = checkAuth(request)
    if (!auth.authorized) {
      return auth.error
    }
    
    const body = await request.json() as { message: string; documentIds: string[] }
    
    // Simulate different responses based on message
    if (body.message.toLowerCase().includes('error')) {
      return HttpResponse.json(
        { error: 'Failed to process message' },
        { status: 500 }
      )
    }
    
    return HttpResponse.json({
      response: `This is a mock response to: "${body.message}". Based on the document content, I can provide relevant information.`,
      sessionId: params.sessionId,
    })
  }),

  http.post('http://localhost:3000/api/genai/summary', async ({ request }) => {
    const auth = checkAuth(request)
    if (!auth.authorized) {
      return auth.error
    }
    
    const body = await request.json() as { documentId: number }
    
    return HttpResponse.json({
      summary: `## Document Summary\n\nThis is a mock summary of document ${body.documentId}.\n\n### Key Points:\n- Point 1\n- Point 2\n- Point 3\n\n### Conclusion\nThe document covers important topics relevant to the subject matter.`,
    })
  }),

  http.get('http://localhost:3000/api/genai/quiz/documents/:documentId', async ({ request, params }) => {
    const auth = checkAuth(request)
    if (!auth.authorized) {
      return auth.error
    }
    
    return HttpResponse.json({
      questions: [
        {
          id: 1,
          question: 'What is the main topic of this document?',
          options: ['Option A', 'Option B', 'Option C', 'Option D'],
          correct_answer: 0,
          explanation: 'This is the correct answer because...',
        },
        {
          id: 2,
          question: 'Which of the following statements is true?',
          options: ['Statement 1', 'Statement 2', 'Statement 3', 'Statement 4'],
          correct_answer: 1,
          explanation: 'This statement is accurate based on the document content.',
        },
      ],
      documentName: 'test-document.pdf',
      documentId: params.documentId,
      status: 'PROCESSED',
    })
  }),

  http.get('http://localhost:3000/api/genai/flashcards/documents/:documentId', async ({ request, params }) => {
    const auth = checkAuth(request)
    if (!auth.authorized) {
      return auth.error
    }
    
    return HttpResponse.json({
      flashcards: [
        {
          id: 1,
          front: 'What is the definition of X?',
          back: 'X is defined as...',
          difficulty: 'medium',
        },
        {
          id: 2,
          front: 'Explain the concept of Y',
          back: 'Y refers to...',
          difficulty: 'easy',
        },
        {
          id: 3,
          front: 'How does Z work?',
          back: 'Z works by...',
          difficulty: 'hard',
        },
      ],
      documentName: 'test-document.pdf',
      documentId: params.documentId,
      status: 'PROCESSED',
    })
  }),

  // Fallback handler for unhandled requests
  http.all('*', ({ request }) => {
    console.warn(`Unhandled ${request.method} request to ${request.url}`)
    return HttpResponse.json(
      { error: 'Not found' },
      { status: 404 }
    )
  }),
]