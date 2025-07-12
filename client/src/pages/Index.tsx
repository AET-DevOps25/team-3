
import { useState, useEffect } from 'react';
import { useToast } from '@/hooks/use-toast';
import DashboardSection from '@/components/DashboardSection';
import HeroSection from '@/components/HeroSection';
import FeaturesSection from '@/components/FeaturesSection';
import Navigation from '@/components/Navigation';
import { useAuth } from '@/contexts/AuthContext';
import { apiService } from '@/lib/api';

interface UploadedFileWithId {
  file: File;
  documentId: string;
}

const Index = () => {
  const { isAuthenticated } = useAuth();
  const [currentView, setCurrentView] = useState<'home' | 'dashboard'>(isAuthenticated ? 'dashboard' : 'home');
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFileWithId[]>([]);
  const [isLoadingDocuments, setIsLoadingDocuments] = useState(false);
  const { toast } = useToast();

  // Fetch user's documents when component loads and user is authenticated
  useEffect(() => {
    const fetchUserDocuments = async () => {
      if (!isAuthenticated) {
        console.log('User not authenticated, skipping document fetch');
        return;
      }
      
      console.log('Fetching user documents...');
      setIsLoadingDocuments(true);
      try {
        const response = await apiService.listDocuments();
        console.log('Documents response:', response);
        const documents = response.documents;
        
        // Convert document list to UploadedFileWithId format
        const filesWithIds: UploadedFileWithId[] = documents.map(doc => ({
          file: new File([], doc.name, { type: doc.type }), // Create a dummy File object
          documentId: doc.id,
        }));
        
        console.log('Converted files with IDs:', filesWithIds);
        setUploadedFiles(filesWithIds);
        
        if (filesWithIds.length > 0) {
          setCurrentView('dashboard');
          toast({
            title: "Documents loaded",
            description: `Found ${filesWithIds.length} document(s) from your previous session`,
          });
        } else {
          console.log('No documents found for user');
        }
      } catch (error) {
        console.error('Error fetching user documents:', error);
        // Don't show error toast if it's just a network issue or no documents
        if (error instanceof Error && !error.message.includes('404')) {
          toast({
            title: "Error loading documents",
            description: "Failed to load your previous documents. You can still upload new ones.",
            variant: "destructive",
          });
        }
      } finally {
        setIsLoadingDocuments(false);
      }
    };

    fetchUserDocuments();
  }, [isAuthenticated, toast]);

  const handleFileUpload = (files: File[], documentIds: string[]) => {
    const filesWithIds: UploadedFileWithId[] = files.map((file, index) => ({
      file,
      documentId: documentIds[index],
    }));
    
    setUploadedFiles(prev => [...prev, ...filesWithIds]);
    setCurrentView('dashboard');
    toast({
      title: "Files uploaded successfully!",
      description: `${files.length} file(s) are being processed...`,
    });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
      <Navigation 
        onBackToHome={() => setCurrentView('home')} 
        showBackButton={currentView === 'dashboard'}
      />
      {currentView === 'home' ? (
        <div className="animate-fade-in">
          <HeroSection onGetStarted={() => setCurrentView('dashboard')} />
          <FeaturesSection />
        </div>
      ) : (
        <div className="animate-fade-in">
          <DashboardSection 
            uploadedFiles={uploadedFiles} 
            onFileUpload={handleFileUpload}
            onBackToHome={() => setCurrentView('home')}
            isLoadingDocuments={isLoadingDocuments}
          />
        </div>
      )}
    </div>
  );
};

export default Index;
