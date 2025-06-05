
import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Upload, FileText, Brain, MessageSquare, BookOpen, Zap, Users, Target } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import UploadSection from '@/components/UploadSection';
import DashboardSection from '@/components/DashboardSection';
import HeroSection from '@/components/HeroSection';
import FeaturesSection from '@/components/FeaturesSection';

const Index = () => {
  const [currentView, setCurrentView] = useState<'home' | 'dashboard'>('home');
  const [uploadedFiles, setUploadedFiles] = useState<File[]>([]);
  const { toast } = useToast();

  const handleFileUpload = (files: File[]) => {
    setUploadedFiles(prev => [...prev, ...files]);
    setCurrentView('dashboard');
    toast({
      title: "Files uploaded successfully!",
      description: `${files.length} file(s) are being processed...`,
    });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
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
          />
        </div>
      )}
    </div>
  );
};

export default Index;
