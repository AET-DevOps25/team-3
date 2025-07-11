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
  id: string;
  originalName: string;
  summary: string | null;
  processedContent: any;
  quizData: any;
  flashcardData: any;
  status: 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'READY' | 'ERROR';
  summaryStatus: 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'READY' | 'ERROR';
  quizStatus: 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'READY' | 'ERROR';
  flashcardStatus: 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'READY' | 'ERROR';
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
  status: 'GENERATING' | 'READY' | 'FAILED';
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
  status: 'GENERATING' | 'READY' | 'FAILED';
  error?: string;
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

  async getQuizForDocument(documentId: string): Promise<QuizApiResponse> {
    console.log('API: Fetching quiz for document:', documentId);
    const response = await fetch(`${this.baseUrl}/api/quiz/documents/${documentId}`);
    console.log('API: Quiz response status:', response.status);
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to get quiz: ${response.status} ${response.statusText}`);
    }
    const data = await response.json();
    console.log('API: Quiz response data:', data);
    
    // Handle error response from backend 
    if (data.error) {
      console.log('API: Quiz status:', data.status, 'Error:', data.error);
      // Return the actual status from backend instead of always setting to FAILED
      return {
        questions: data.questions || [],
        documentName: data.documentName || 'Unknown Document',
        documentId: documentId,
        status: data.status || 'FAILED',
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
    console.log('API: Fetching flashcards for document:', documentId);
    const response = await fetch(`${this.baseUrl}/api/flashcards/documents/${documentId}`);
    console.log('API: Flashcard response status:', response.status);
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `Failed to get flashcards: ${response.status} ${response.statusText}`);
    }
    const data = await response.json();
    console.log('API: Flashcard response data:', data);
    
    // Handle error response from backend
    if (data.error) {
      console.log('API: Flashcards not ready yet:', data.error);
      return {
        flashcards: [],
        documentName: data.documentName || 'Unknown Document',
        documentId: documentId,
        status: 'FAILED',
        error: data.error
      };
    }
    
    return {
      flashcards: data.flashcards || [],
      documentName: data.documentName || 'Unknown Document',
      documentId: documentId,
      status: data.status || 'FAILED'
    };
  }
}

export const apiService = new ApiService();

export default apiService; 