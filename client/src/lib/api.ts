// API service for communicating with the StudyMate microservices
const AUTH_SERVICE_URL = 'http://localhost:8081';
const DOCUMENT_SERVICE_URL = 'http://localhost:8082';
const AI_SERVICE_URL = 'http://localhost:8083';

// Common status type used throughout the application
export type DocumentStatus = 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'READY' | 'ERROR';

// Authentication interfaces
export interface UserRegistrationRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface UserLoginRequest {
  username: string;
  password: string;
}

export interface UserResponse {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  role: 'USER' | 'ADMIN';
  isActive: boolean;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface JwtAuthenticationResponse {
  token: string;
  refreshToken: string;
  expiresIn: number;
  user: UserResponse;
}

export interface JwtRefreshRequest {
  refreshToken: string;
}

export interface JwtRefreshResponse {
  token: string;
  refreshToken: string;
  expiresIn: number;
}

export interface UserUpdateRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
}

export interface UserPasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}

export interface AuthErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}

export interface ValidationErrorResponse {
  error: string;
  message: string;
  fieldErrors?: { [key: string]: string };
  timestamp: string;
}

// Token management
class TokenManager {
  private static readonly TOKEN_KEY = 'auth_token';
  private static readonly REFRESH_TOKEN_KEY = 'refresh_token';

  static getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  static setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  static getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  static setRefreshToken(refreshToken: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
  }

  static clearTokens(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }

  static isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const currentTime = Date.now() / 1000;
      return payload.exp < currentTime;
    } catch (error) {
      return true;
    }
  }

  static getTokenExpirationTime(token: string): number | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000; // Convert to milliseconds
    } catch (error) {
      return null;
    }
  }
}

export interface DocumentUploadResponse {
  documentIds: string[];
  status: string;
}

export interface DocumentInfo {
  id: string;
  name: string;
  size: number;
  uploadDate: string;
  type: string;
  status: DocumentStatus;
}

export interface DocumentListResponse {
  documents: DocumentInfo[];
}

export interface DocumentStatusResponse {
  documentId: string;
  status: DocumentStatus;
  documentName: string;
  uploadDate: string;
}

export interface DocumentContentResponse {
  id: string;
  originalName: string;
  summary: string | null;
  processedContent: any;
  quizData: any;
  flashcardData: any;
  status: DocumentStatus;
  summaryStatus: DocumentStatus;
  quizStatus: DocumentStatus;
  flashcardStatus: DocumentStatus;
  uploadDate: string;
  updatedAt: string;
}

export interface FlashcardModel {
  question: string;
  answer: string;
  difficulty: string;
}

export interface FlashcardResponse {
  response: {
    flashcards: FlashcardModel[];
  };
}

export interface FlashcardApiResponse {
  flashcards: FlashcardModel[];
  documentName: string;
  documentId: string;
  status: DocumentStatus;
  error?: string;
}

// Chat interfaces
export interface ChatMessage {
  id: string;
  content: string;
  sender: 'user' | 'bot';
  timestamp: string;
  sources?: string[];
  documentReferences?: DocumentReference[];
}

export interface DocumentReference {
  documentId: string;
  documentName: string;
  relevantPages?: number[];
}

export interface ChatSessionRequest {
  documentIds: string[];
}

export interface ChatSessionResponse {
  sessionId: string;
  messages: ChatMessage[];
  documentsInContext: string[];
}

export interface ChatMessageRequest {
  message: string;
  documentIds?: string[];
}

export interface ChatMessageResponse {
  id: string;
  content: string;
  sender: string;
  timestamp: string;
  sources?: string[];
  documentReferences?: DocumentReference[];
}

export interface QuizApiResponse {
  questions: any[];
  documentName: string;
  documentId: string;
  status: DocumentStatus;
  error?: string;
}

// Main API service class
class ApiService {
  private authServiceUrl: string;
  private documentServiceUrl: string;
  private aiServiceUrl: string;
  private refreshing: boolean = false;

  constructor(
    authServiceUrl: string = AUTH_SERVICE_URL,
    documentServiceUrl: string = DOCUMENT_SERVICE_URL,
    aiServiceUrl: string = AI_SERVICE_URL
  ) {
    this.authServiceUrl = authServiceUrl;
    this.documentServiceUrl = documentServiceUrl;
    this.aiServiceUrl = aiServiceUrl;
  }

  private getHeaders(includeAuth: boolean = true): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };

    if (includeAuth) {
      const token = TokenManager.getToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
    }

    return headers;
  }

  private async authenticatedFetch(url: string, options: RequestInit = {}): Promise<Response> {
    const token = TokenManager.getToken();
    
    if (!token) {
      throw new Error('No authentication token available');
    }

    // Check if token is expired
    if (TokenManager.isTokenExpired(token)) {
      if (!this.refreshing) {
        this.refreshing = true;
        try {
          await this.refreshToken();
        } finally {
          this.refreshing = false;
        }
      }
    }

    const response = await fetch(url, {
      ...options,
      headers: this.getHeaders(),
    });

    if (response.status === 401) {
      // Token might be invalid, try to refresh
      if (!this.refreshing) {
        this.refreshing = true;
        try {
          await this.refreshToken();
          // Retry the request with new token
          const newResponse = await fetch(url, {
            ...options,
            headers: this.getHeaders(),
          });
          return newResponse;
        } finally {
          this.refreshing = false;
        }
      }
    }

    return response;
  }

  // Authentication methods (Auth Service)
  async register(userData: UserRegistrationRequest): Promise<JwtAuthenticationResponse> {
    const response = await fetch(`${this.authServiceUrl}/api/auth/register`, {
      method: 'POST',
      headers: this.getHeaders(false),
      body: JSON.stringify(userData),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Registration failed');
    }

    const data = await response.json();
    TokenManager.setToken(data.token);
    TokenManager.setRefreshToken(data.refreshToken);
    return data;
  }

  async login(credentials: UserLoginRequest): Promise<JwtAuthenticationResponse> {
    const response = await fetch(`${this.authServiceUrl}/api/auth/login`, {
      method: 'POST',
      headers: this.getHeaders(false),
      body: JSON.stringify(credentials),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Login failed');
    }

    const data = await response.json();
    TokenManager.setToken(data.token);
    TokenManager.setRefreshToken(data.refreshToken);
    return data;
  }

  async refreshToken(): Promise<JwtRefreshResponse> {
    const refreshToken = TokenManager.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await fetch(`${this.authServiceUrl}/api/auth/refresh`, {
      method: 'POST',
      headers: this.getHeaders(false),
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      TokenManager.clearTokens();
      throw new Error('Token refresh failed');
    }

    const data = await response.json();
    TokenManager.setToken(data.token);
    TokenManager.setRefreshToken(data.refreshToken);
    return data;
  }

  async logout(): Promise<void> {
    TokenManager.clearTokens();
  }

  async getCurrentUser(): Promise<UserResponse> {
    const response = await this.authenticatedFetch(`${this.authServiceUrl}/api/auth/profile`);
    
    if (!response.ok) {
      throw new Error('Failed to get user profile');
    }

    return response.json();
  }

  async updateUser(userData: UserUpdateRequest): Promise<UserResponse> {
    const response = await this.authenticatedFetch(`${this.authServiceUrl}/api/auth/profile`, {
      method: 'PUT',
      body: JSON.stringify(userData),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Failed to update user');
    }

    return response.json();
  }

  async changePassword(passwordData: UserPasswordChangeRequest): Promise<{ message: string }> {
    const response = await this.authenticatedFetch(`${this.authServiceUrl}/api/auth/change-password`, {
      method: 'POST',
      body: JSON.stringify(passwordData),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Failed to change password');
    }

    return response.json();
  }

  isAuthenticated(): boolean {
    const token = TokenManager.getToken();
    return token !== null && !TokenManager.isTokenExpired(token);
  }

  getCurrentUserFromToken(): Partial<UserResponse> | null {
    const token = TokenManager.getToken();
    if (!token) return null;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return {
        id: payload.sub,
        username: payload.username,
        email: payload.email,
        firstName: payload.firstName,
        lastName: payload.lastName,
        role: payload.role,
      };
    } catch (error) {
      return null;
    }
  }

  // Document methods (Document Service)
  async uploadFiles(files: File[]): Promise<DocumentUploadResponse> {
    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });

    const response = await this.authenticatedFetch(`${this.documentServiceUrl}/api/documents/upload`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${TokenManager.getToken()}`,
      },
      body: formData,
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Upload failed');
    }

    return response.json();
  }

  async getDocumentStatus(documentId: string): Promise<DocumentStatusResponse> {
    const response = await this.authenticatedFetch(`${this.documentServiceUrl}/api/documents/${documentId}/status`);
    
    if (!response.ok) {
      throw new Error('Failed to get document status');
    }

    return response.json();
  }

  async listDocuments(): Promise<DocumentListResponse> {
    const response = await this.authenticatedFetch(`${this.documentServiceUrl}/api/documents`);
    
    if (!response.ok) {
      throw new Error('Failed to list documents');
    }

    return response.json();
  }

  async downloadDocument(documentId: string): Promise<Blob> {
    const response = await this.authenticatedFetch(`${this.documentServiceUrl}/api/documents/${documentId}/download`);
    
    if (!response.ok) {
      throw new Error('Failed to download document');
    }

    return response.blob();
  }

  async deleteDocument(documentId: string): Promise<void> {
    const response = await this.authenticatedFetch(`${this.documentServiceUrl}/api/documents/${documentId}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error('Failed to delete document');
    }
  }

  async getDocumentContent(documentId: string): Promise<DocumentContentResponse> {
    const response = await this.authenticatedFetch(`${this.documentServiceUrl}/api/documents/${documentId}/content`);
    
    if (!response.ok) {
      throw new Error('Failed to get document content');
    }

    return response.json();
  }

  async updateDocumentContent(documentId: string, summary: string, processedContent: any): Promise<{message: string}> {
    const response = await this.authenticatedFetch(`${this.documentServiceUrl}/api/documents/${documentId}/content`, {
      method: 'PUT',
      body: JSON.stringify({ summary, processedContent }),
    });

    if (!response.ok) {
      throw new Error('Failed to update document content');
    }

    return response.json();
  }

  // Chat methods (AI Service)
  async createChatSession(documentIds: string[]): Promise<ChatSessionResponse> {
    const response = await this.authenticatedFetch(`${this.aiServiceUrl}/api/ai/chat/session`, {
      method: 'POST',
      body: JSON.stringify({ documentIds }),
    });

    if (!response.ok) {
      throw new Error('Failed to create chat session');
    }

    return response.json();
  }

  async getChatSession(sessionId: string): Promise<ChatSessionResponse> {
    const response = await this.authenticatedFetch(`${this.aiServiceUrl}/api/ai/chat/session/${sessionId}`);
    
    if (!response.ok) {
      throw new Error('Failed to get chat session');
    }

    return response.json();
  }

  async sendMessage(sessionId: string, message: string, documentIds: string[] = []): Promise<ChatMessageResponse> {
    const response = await this.authenticatedFetch(`${this.aiServiceUrl}/api/ai/chat/message`, {
      method: 'POST',
      body: JSON.stringify({ sessionId, message, documentIds }),
    });

    if (!response.ok) {
      throw new Error('Failed to send message');
    }

    return response.json();
  }

  // Quiz methods (AI Service)
  async getQuizForDocument(documentId: string): Promise<QuizApiResponse> {
    const response = await this.authenticatedFetch(`${this.aiServiceUrl}/api/ai/generate-quiz`, {
      method: 'POST',
      body: JSON.stringify({ documentId }),
    });

    if (!response.ok) {
      throw new Error('Failed to get quiz for document');
    }

    return response.json();
  }

  // Flashcard methods (AI Service)
  async getFlashcardsForDocument(documentId: string): Promise<FlashcardApiResponse> {
    const response = await this.authenticatedFetch(`${this.aiServiceUrl}/api/ai/generate-flashcards`, {
      method: 'POST',
      body: JSON.stringify({ documentId }),
    });

    if (!response.ok) {
      throw new Error('Failed to get flashcards for document');
    }

    return response.json();
  }
}

// Export singleton instance
export const apiService = new ApiService();
export { TokenManager }; 