// API service for communicating with the Spring Boot backend
const API_BASE_URL = 'http://localhost:8082';

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

class ApiService {
  private baseUrl: string;
  private refreshing: boolean = false;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  // Get headers with authorization if available
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

  // Enhanced fetch with automatic token refresh
  private async authenticatedFetch(url: string, options: RequestInit = {}): Promise<Response> {
    const token = TokenManager.getToken();
    
    // Add authorization header
    if (token) {
      options.headers = {
        ...options.headers,
        'Authorization': `Bearer ${token}`,
      };
    }

    let response = await fetch(url, options);

    // If token is expired, try to refresh
    if (response.status === 401 && token && !this.refreshing) {
      this.refreshing = true;
      
      try {
        await this.refreshToken();
        const newToken = TokenManager.getToken();
        
        if (newToken) {
          // Retry the original request with new token
          options.headers = {
            ...options.headers,
            'Authorization': `Bearer ${newToken}`,
          };
          response = await fetch(url, options);
        }
      } catch (error) {
        // Refresh failed, clear tokens and redirect to login
        TokenManager.clearTokens();
        window.location.href = '/login';
        throw error;
      } finally {
        this.refreshing = false;
      }
    }

    return response;
  }

  // Authentication methods
  async register(userData: UserRegistrationRequest): Promise<JwtAuthenticationResponse> {
    const response = await fetch(`${this.baseUrl}/api/auth/register`, {
      method: 'POST',
      headers: this.getHeaders(false),
      body: JSON.stringify(userData),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Registration failed: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    
    // Store tokens
    TokenManager.setToken(data.token);
    TokenManager.setRefreshToken(data.refreshToken);
    
    return data;
  }

  async login(credentials: UserLoginRequest): Promise<JwtAuthenticationResponse> {
    const response = await fetch(`${this.baseUrl}/api/auth/login`, {
      method: 'POST',
      headers: this.getHeaders(false),
      body: JSON.stringify(credentials),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Login failed: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    
    // Store tokens
    TokenManager.setToken(data.token);
    TokenManager.setRefreshToken(data.refreshToken);
    
    return data;
  }

  async refreshToken(): Promise<JwtRefreshResponse> {
    const refreshToken = TokenManager.getRefreshToken();
    
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await fetch(`${this.baseUrl}/api/auth/refresh`, {
      method: 'POST',
      headers: this.getHeaders(false),
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Token refresh failed: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    
    // Update tokens
    TokenManager.setToken(data.token);
    TokenManager.setRefreshToken(data.refreshToken);
    
    return data;
  }

  async logout(): Promise<void> {
    TokenManager.clearTokens();
  }

  async getCurrentUser(): Promise<UserResponse> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/auth/me`);

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Failed to get current user: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async updateUser(userData: UserUpdateRequest): Promise<UserResponse> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/auth/me`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: JSON.stringify(userData),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Failed to update user: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async changePassword(passwordData: UserPasswordChangeRequest): Promise<{ message: string }> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/auth/me/password`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: JSON.stringify(passwordData),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Failed to change password: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    const token = TokenManager.getToken();
    return token !== null && !TokenManager.isTokenExpired(token);
  }

  // Get current user from token
  getCurrentUserFromToken(): Partial<UserResponse> | null {
    const token = TokenManager.getToken();
    
    if (!token) {
      return null;
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return {
        username: payload.sub,
        // Add other user info if available in token
      };
    } catch (error) {
      return null;
    }
  }

  async uploadFiles(files: File[]): Promise<DocumentUploadResponse> {
    const formData = new FormData();
    
    // Append all files to the form data
    files.forEach((file) => {
      formData.append('files', file);
    });

    const token = TokenManager.getToken();
    const headers: HeadersInit = {};
    
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${this.baseUrl}/api/documents/upload`, {
      method: 'POST',
      headers,
      body: formData,
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Upload failed: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getDocumentStatus(documentId: string): Promise<DocumentStatusResponse> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/documents/${documentId}/status`);
    
    if (!response.ok) {
      throw new Error(`Failed to get document status: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async listDocuments(): Promise<DocumentListResponse> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/documents`);
    
    if (!response.ok) {
      throw new Error(`Failed to list documents: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async downloadDocument(documentId: string): Promise<Blob> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/documents/${documentId}/download`);
    
    if (!response.ok) {
      throw new Error(`Failed to download document: ${response.status} ${response.statusText}`);
    }

    return response.blob();
  }

  async deleteDocument(documentId: string): Promise<void> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/documents/${documentId}`, {
      method: 'DELETE',
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to delete document: ${response.status} ${response.statusText}`);
    }
  }

  async getDocumentContent(documentId: string): Promise<DocumentContentResponse> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/documents/${documentId}/content`);
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to get document content: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async updateDocumentContent(documentId: string, summary: string, processedContent: any): Promise<{message: string}> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/documents/${documentId}/content`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: JSON.stringify({
        summary,
        processedContent,
      }),
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to update document content: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  // Chat methods
  async createChatSession(documentIds: string[]): Promise<ChatSessionResponse> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/chat/sessions`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify({ documentIds }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to create chat session: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getChatSession(sessionId: string): Promise<ChatSessionResponse> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/chat/sessions/${sessionId}`);
    
    if (!response.ok) {
      throw new Error(`Failed to get chat session: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async sendMessage(sessionId: string, message: string, documentIds: string[] = []): Promise<ChatMessageResponse> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/chat/sessions/${sessionId}/messages`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify({ 
        message,
        documentIds 
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to send message: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getQuizForDocument(documentId: string): Promise<QuizApiResponse> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/quiz/documents/${documentId}`);
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to get quiz: ${response.status} ${response.statusText}`);
    }
    
    const data = await response.json();
    
    // Handle error response from backend 
    if (data.error) {
      return {
        questions: data.questions || [],
        documentName: data.documentName || 'Unknown Document',
        documentId: documentId,
        status: data.status || 'ERROR',
        error: data.error
      };
    }
    
    // Map correct_answer to correctAnswer for each question
    if (data && data.questions && Array.isArray(data.questions)) {
      data.questions = data.questions.map(q => ({
        ...q,
        correctAnswer: q.correct_answer,
      }));
    }
    return data;
  }

  async getFlashcardsForDocument(documentId: string): Promise<FlashcardApiResponse> {
    const response = await this.authenticatedFetch(`${this.baseUrl}/api/flashcards/documents/${documentId}`);
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to get flashcards: ${response.status} ${response.statusText}`);
    }
    
    const data = await response.json();
    
    // Handle error response from backend
    if (data.error) {
      return {
        flashcards: [],
        documentName: data.documentName || 'Unknown Document',
        documentId: documentId,
        status: 'ERROR',
        error: data.error
      };
    }
    
    return {
      flashcards: data.flashcards || [],
      documentName: data.documentName || 'Unknown Document',
      documentId: documentId,
      status: data.status || 'ERROR'
    };
  }
}

export const apiService = new ApiService();
export { TokenManager };

export default apiService; 