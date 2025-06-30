import { useState, useCallback } from 'react';
import type { DragEvent, ChangeEvent } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Upload, FileText, X, CheckCircle, ArrowRight, Loader2, AlertCircle } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { apiService, DocumentUploadResponse } from '@/lib/api';

interface UploadedFileInfo {
  file: File;
  id?: string;
  status: 'pending' | 'uploading' | 'success' | 'error';
  error?: string;
}

interface UploadSectionProps {
  onFileUpload: (files: File[], documentIds: string[]) => void;
  uploadedFiles: File[];
  onContinue: () => void;
}

const UploadSection = ({ onFileUpload, uploadedFiles, onContinue }: UploadSectionProps) => {
  const [dragActive, setDragActive] = useState(false);
  const [uploadingFiles, setUploadingFiles] = useState<UploadedFileInfo[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const { toast } = useToast();

  const handleDrag = useCallback((e: DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  }, []);

  const handleDrop = useCallback((e: DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    const files = Array.from(e.dataTransfer.files) as File[];
    if (files.length > 0) {
      handleFileUpload(files);
    }
  }, []);

  const handleFileInput = (e: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []) as File[];
    if (files.length > 0) {
      handleFileUpload(files);
    }
  };

  const handleFileUpload = async (files: File[]) => {
    // Validate file types
    const allowedTypes = ['.pdf', '.ppt', '.pptx', '.doc', '.docx', '.txt'];
    const invalidFiles = files.filter(file => {
      const extension = '.' + file.name.split('.').pop()?.toLowerCase();
      return !allowedTypes.includes(extension);
    });

    if (invalidFiles.length > 0) {
      toast({
        title: 'Invalid file types',
        description: `Please upload only PDF, PPT, PPTX, DOC, DOCX, or TXT files.`,
        variant: 'destructive',
      });
      return;
    }

    // Initialize upload state
    const fileInfos: UploadedFileInfo[] = files.map(file => ({
      file,
      status: 'pending' as const,
    }));
    
    setUploadingFiles(prev => [...prev, ...fileInfos]);
    setIsUploading(true);

    try {
      // Update status to uploading
      setUploadingFiles(prev => 
        prev.map(f => 
          files.includes(f.file) ? { ...f, status: 'uploading' as const } : f
        )
      );

      // Upload files to backend
      const response: DocumentUploadResponse = await apiService.uploadFiles(files);
      
      // Update status to success
      setUploadingFiles(prev => 
        prev.map(f => {
          if (files.includes(f.file)) {
            const index = files.indexOf(f.file);
            return {
              ...f,
              status: 'success' as const,
              id: response.documentIds[index],
            };
          }
          return f;
        })
      );

      // Call the original callback for parent component
      onFileUpload(files, response.documentIds);

      toast({
        title: 'Upload successful!',
        description: `Successfully uploaded ${files.length} file(s). Status: ${response.status}`,
      });

    } catch (error) {
      console.error('Upload error:', error);
      
      // Update status to error
      setUploadingFiles(prev => 
        prev.map(f => 
          files.includes(f.file) 
            ? { ...f, status: 'error' as const, error: error instanceof Error ? error.message : 'Upload failed' }
            : f
        )
      );

      toast({
        title: 'Upload failed',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
        variant: 'destructive',
      });
    } finally {
      setIsUploading(false);
    }
  };

  const removeFile = (index: number, fileInfo?: UploadedFileInfo) => {
    if (fileInfo?.id) {
      // If file has been uploaded to backend, delete it
      apiService.deleteDocument(fileInfo.id).catch(error => {
        console.error('Error deleting file:', error);
        toast({
          title: 'Delete failed',
          description: 'Failed to delete file from server',
          variant: 'destructive',
        });
      });
    }

    setUploadingFiles(prev => prev.filter((_, i) => i !== index));
    
    toast({
      title: "File removed",
      description: "File has been removed from the upload list.",
    });
  };

  const getStatusIcon = (status: UploadedFileInfo['status']) => {
    switch (status) {
      case 'uploading':
        return <Loader2 className="h-4 w-4 text-blue-500 animate-spin" />;
      case 'success':
        return <CheckCircle className="h-4 w-4 text-green-500" />;
      case 'error':
        return <AlertCircle className="h-4 w-4 text-red-500" />;
      default:
        return <FileText className="h-4 w-4 text-gray-500" />;
    }
  };

  const getStatusText = (fileInfo: UploadedFileInfo) => {
    switch (fileInfo.status) {
      case 'uploading':
        return 'Uploading...';
      case 'success':
        return `Uploaded (ID: ${fileInfo.id?.substring(0, 8)}...)`;
      case 'error':
        return fileInfo.error || 'Upload failed';
      default:
        return 'Pending';
    }
  };

  const successfulUploads = uploadingFiles.filter(f => f.status === 'success');
  const hasSuccessfulUploads = successfulUploads.length > 0;

  return (
    <div className="space-y-6">
      {/* Upload Card */}
      <Card className="border-2 border-dashed border-gray-300 hover:border-blue-400 transition-colors">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-bold text-gray-900">Upload Your Course Materials</CardTitle>
          <CardDescription className="text-lg text-gray-600">
            Upload lecture slides, PDFs, or transcripts to get started
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div
            className={`relative border-2 border-dashed rounded-xl p-12 text-center transition-all duration-300 ${
              dragActive 
                ? 'border-blue-500 bg-blue-50' 
                : 'border-gray-300 hover:border-blue-400 hover:bg-blue-50/30'
            } ${isUploading ? 'opacity-50 pointer-events-none' : ''}`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
          >
            <input
              type="file"
              multiple
              accept=".pdf,.ppt,.pptx,.doc,.docx,.txt"
              onChange={handleFileInput}
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
              disabled={isUploading}
            />
            
            <div className="space-y-4">
              <div className="mx-auto w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center">
                {isUploading ? (
                  <Loader2 className="h-8 w-8 text-blue-600 animate-spin" />
                ) : (
                  <Upload className="h-8 w-8 text-blue-600" />
                )}
              </div>
              
              <div>
                <p className="text-xl font-semibold text-gray-900 mb-2">
                  {isUploading ? 'Uploading files...' : 'Drop your files here, or click to browse'}
                </p>
                <p className="text-gray-600">
                  Supports PDF, PPT, PPTX, DOC, DOCX, TXT files
                </p>
              </div>
              
              <Button 
                variant="outline" 
                className="bg-white hover:bg-blue-50 border-blue-200 text-blue-600"
                disabled={isUploading}
              >
                {isUploading ? 'Uploading...' : 'Choose Files'}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Uploaded Files List */}
      {uploadingFiles.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <FileText className="h-5 w-5 text-blue-500" />
              <span>Files ({uploadingFiles.length})</span>
              {isUploading && (
                <Loader2 className="h-4 w-4 text-blue-500 animate-spin ml-2" />
              )}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {uploadingFiles.map((fileInfo, index) => (
                <div key={index} className={`flex items-center justify-between p-3 rounded-lg ${
                  fileInfo.status === 'error' ? 'bg-red-50 border border-red-200' :
                  fileInfo.status === 'success' ? 'bg-green-50 border border-green-200' :
                  'bg-gray-50'
                }`}>
                  <div className="flex items-center space-x-3">
                    {getStatusIcon(fileInfo.status)}
                    <div>
                      <p className="font-medium text-gray-900">{fileInfo.file.name}</p>
                      <div className="flex items-center space-x-2 text-sm">
                        <span className="text-gray-500">
                          {(fileInfo.file.size / 1024 / 1024).toFixed(2)} MB
                        </span>
                        <span className="text-gray-300">•</span>
                        <span className={`${
                          fileInfo.status === 'error' ? 'text-red-600' :
                          fileInfo.status === 'success' ? 'text-green-600' :
                          'text-blue-600'
                        }`}>
                          {getStatusText(fileInfo)}
                        </span>
                      </div>
                    </div>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeFile(index, fileInfo)}
                    className="text-gray-400 hover:text-red-500"
                    disabled={fileInfo.status === 'uploading'}
                  >
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              ))}
            </div>
            
            {hasSuccessfulUploads && (
              <div className="mt-6 flex justify-center">
                <Button 
                  onClick={onContinue}
                  className="bg-blue-600 hover:bg-blue-700 px-8 py-3 text-lg"
                  disabled={isUploading}
                >
                  Continue to AI Processing
                  <ArrowRight className="ml-2 h-5 w-5" />
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Instructions */}
      <div className="grid md:grid-cols-3 gap-6">
        <Card className="text-center">
          <CardContent className="pt-6">
            <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-blue-600 font-bold text-lg">1</span>
            </div>
            <h3 className="font-semibold text-gray-900 mb-2">Upload Materials</h3>
            <p className="text-gray-600 text-sm">Upload your lecture slides, notes, or transcripts</p>
          </CardContent>
        </Card>
        
        <Card className="text-center">
          <CardContent className="pt-6">
            <div className="w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-purple-600 font-bold text-lg">2</span>
            </div>
            <h3 className="font-semibold text-gray-900 mb-2">AI Processing</h3>
            <p className="text-gray-600 text-sm">Our AI analyzes and creates interactive content</p>
          </CardContent>
        </Card>
        
        <Card className="text-center">
          <CardContent className="pt-6">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-green-600 font-bold text-lg">3</span>
            </div>
            <h3 className="font-semibold text-gray-900 mb-2">Start Learning</h3>
            <p className="text-gray-600 text-sm">Access summaries, quizzes, flashcards, and chat</p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default UploadSection;
