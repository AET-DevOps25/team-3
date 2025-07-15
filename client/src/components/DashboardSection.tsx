import { useState, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ArrowLeft, FileText, Brain, MessageSquare, BookOpen, Upload, Sparkles, Loader2 } from 'lucide-react';
import UploadSection from '@/components/UploadSection';
import SummaryTab from '@/components/SummaryTab';
import QuizTab from '@/components/QuizTab';
import FlashcardsTab from '@/components/FlashcardsTab';
import ChatTab from '@/components/ChatTab';

interface UploadedFileWithId {
  file: File;
  documentId: string;
}

interface QuizData {
  questions: any[];
  documentName: string;
  documentId: string;
  status?: 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'ERROR' | 'PENDING';
  error?: string;
}

interface DashboardSectionProps {
  uploadedFiles: UploadedFileWithId[];
  onFileUpload: (files: File[], documentIds: string[]) => void;
  onBackToHome: () => void;
  isLoadingDocuments?: boolean;
}

const DashboardSection = ({ uploadedFiles, onFileUpload, onBackToHome, isLoadingDocuments = false }: DashboardSectionProps) => {
  const [activeTab, setActiveTab] = useState('upload');
  const [quizzes, setQuizzes] = useState<QuizData[]>([]);
  const [answers, setAnswers] = useState<{ [documentId: string]: { [questionIndex: number]: string | number } }>({});
  
  // Extract files and document IDs with memoization to prevent unnecessary re-renders
  const files = useMemo(() => {
    return uploadedFiles.map(item => item.file);
  }, [uploadedFiles]);
  const documentIds = useMemo(() => {
    return uploadedFiles.map(item => item.documentId);
  }, [uploadedFiles]);

  // Show loading state while documents are being fetched
  if (isLoadingDocuments) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-blue-500" />
          <p className="text-gray-600">Loading your documents...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <Button 
                variant="ghost" 
                onClick={onBackToHome}
                className="hover:bg-gray-100"
              >
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Home
              </Button>
              <div className="flex items-center space-x-3">
                <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-500 rounded-lg flex items-center justify-center">
                  <Sparkles className="h-5 w-5 text-white" />
                </div>
                <h1 className="text-2xl font-bold text-gray-900">StudyMate Dashboard</h1>
              </div>
            </div>
            <div className="text-sm text-gray-600">
              {files.length} file(s) uploaded
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-6 py-8">
        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="grid w-full grid-cols-5 bg-white p-1 rounded-xl shadow-sm border">
            <TabsTrigger value="upload" className="flex items-center space-x-2 data-[state=active]:bg-blue-500 data-[state=active]:text-white">
              <Upload className="h-4 w-4" />
              <span className="hidden sm:inline">Upload</span>
            </TabsTrigger>
            <TabsTrigger value="summary" className="flex items-center space-x-2 data-[state=active]:bg-blue-500 data-[state=active]:text-white">
              <FileText className="h-4 w-4" />
              <span className="hidden sm:inline">Summary</span>
            </TabsTrigger>
            <TabsTrigger value="quiz" className="flex items-center space-x-2 data-[state=active]:bg-blue-500 data-[state=active]:text-white">
              <Brain className="h-4 w-4" />
              <span className="hidden sm:inline">Quiz</span>
            </TabsTrigger>
            <TabsTrigger value="flashcards" className="flex items-center space-x-2 data-[state=active]:bg-blue-500 data-[state=active]:text-white">
              <BookOpen className="h-4 w-4" />
              <span className="hidden sm:inline">Flashcards</span>
            </TabsTrigger>
            <TabsTrigger value="chat" className="flex items-center space-x-2 data-[state=active]:bg-blue-500 data-[state=active]:text-white">
              <MessageSquare className="h-4 w-4" />
              <span className="hidden sm:inline">Chat</span>
            </TabsTrigger>
          </TabsList>

          <TabsContent value="upload" className="animate-fade-in">
            <UploadSection 
              onFileUpload={onFileUpload} 
              uploadedFiles={files}
              onContinue={() => setActiveTab('summary')}
            />
          </TabsContent>

          <TabsContent value="summary" className="animate-fade-in">
            <SummaryTab uploadedFiles={files} documentIds={documentIds} />
          </TabsContent>

          <TabsContent value="quiz" className="animate-fade-in">
            <QuizTab uploadedFiles={files} documentIds={documentIds} quizzes={quizzes} setQuizzes={setQuizzes} answers={answers} setAnswers={setAnswers} />
          </TabsContent>

          <TabsContent value="flashcards" className="animate-fade-in">
            <FlashcardsTab uploadedFiles={files} documentIds={documentIds} />
          </TabsContent>

          <TabsContent value="chat" className="animate-fade-in">
            <ChatTab uploadedFiles={files} documentIds={documentIds} />
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
};

export default DashboardSection;
