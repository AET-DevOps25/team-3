// API service for communicating with the Spring Boot backend
const API_BASE_URL = 'http://localhost:8082';

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
  status: 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'READY' | 'ERROR';
}

export interface DocumentListResponse {
  documents: DocumentInfo[];
}

export interface DocumentStatusResponse {
  documentId: string;
  status: 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'READY' | 'ERROR';
  documentName: string;
  uploadDate: string;
}

export interface DocumentContentResponse {
  documentId: string;
  summary: string | null;
  processedContent: any;
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

class ApiService {
  private baseUrl: string;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  async uploadFiles(files: File[]): Promise<DocumentUploadResponse> {
    const formData = new FormData();
    
    // Append all files to the form data
    files.forEach((file) => {
      formData.append('files', file);
    });

    const response = await fetch(`${this.baseUrl}/api/documents/upload`, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Upload failed: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getDocumentStatus(documentId: string): Promise<DocumentStatusResponse> {
    const response = await fetch(`${this.baseUrl}/api/documents/${documentId}/status`);
    
    if (!response.ok) {
      throw new Error(`Failed to get document status: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async listDocuments(): Promise<DocumentListResponse> {
    const response = await fetch(`${this.baseUrl}/api/documents`);
    
    if (!response.ok) {
      throw new Error(`Failed to list documents: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async downloadDocument(documentId: string): Promise<Blob> {
    const response = await fetch(`${this.baseUrl}/api/documents/${documentId}/download`);
    
    if (!response.ok) {
      throw new Error(`Failed to download document: ${response.status} ${response.statusText}`);
    }

    return response.blob();
  }

  async deleteDocument(documentId: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/api/documents/${documentId}`, {
      method: 'DELETE',
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to delete document: ${response.status} ${response.statusText}`);
    }
  }

  async getDocumentContent(documentId: string): Promise<DocumentContentResponse> {
    const response = await fetch(`${this.baseUrl}/api/documents/${documentId}/content`);
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to get document content: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async updateDocumentContent(documentId: string, summary: string, processedContent: any): Promise<{message: string}> {
    const response = await fetch(`${this.baseUrl}/api/documents/${documentId}/content`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
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
    const response = await fetch(`${this.baseUrl}/api/chat/sessions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ documentIds }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to create chat session: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getChatSession(sessionId: string): Promise<ChatSessionResponse> {
    const response = await fetch(`${this.baseUrl}/api/chat/sessions/${sessionId}`);
    
    if (!response.ok) {
      throw new Error(`Failed to get chat session: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async sendMessage(sessionId: string, message: string, documentIds: string[] = []): Promise<ChatMessageResponse> {
    const response = await fetch(`${this.baseUrl}/api/chat/sessions/${sessionId}/messages`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
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

  async getQuizForDocument(documentId: string): Promise<any> {
    console.log('API: Fetching quiz for document:', documentId);
    const response = await fetch(`${this.baseUrl}/api/quiz/documents/${documentId}`);
    console.log('API: Quiz response status:', response.status);
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to get quiz: ${response.status} ${response.statusText}`);
    }
    const data = await response.json();
    // Map correct_answer to correctAnswer for each question
    if (data && data.questions && Array.isArray(data.questions)) {
      data.questions = data.questions.map(q => ({
        ...q,
        correctAnswer: q.correct_answer,
      }));
    }
    return data;
  }
}

export const apiService = new ApiService();

export default apiService; 