import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { FileText, Clock, BookOpen, Download, Sparkles, Loader2, RefreshCw } from 'lucide-react';
import { apiService, DocumentInfo } from '../lib/api';
import { useToast } from '../hooks/use-toast';
import jsPDF from 'jspdf';
import ReactMarkdown from 'react-markdown';

interface SummaryTabProps {
  uploadedFiles: File[];
  documentIds: string[];
}

interface DocumentSummary {
  id: string;
  name: string;
  summary: string | null;
  status: string;
  summaryStatus: string;
  uploadDate: string;
  readTime?: string;
  keyPoints?: string[];
}

const SummaryTab = ({ uploadedFiles, documentIds }: SummaryTabProps) => {

  
  const [summaries, setSummaries] = useState<DocumentSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const { toast } = useToast();

  // Helper function for status badge styling
  const getStatusBadgeStyle = (status: string) => {
    switch (status) {
      case 'PROCESSED':
        return 'bg-green-100 text-green-600';
      case 'PROCESSING':
        return 'bg-blue-100 text-blue-600';
      case 'ERROR':
        return 'bg-red-100 text-red-600';
      default:
        return 'bg-gray-100 text-gray-600';
    }
  };

  // Helper function for card styling
  const getCardStyle = (status: string) => {
    if (status === 'ERROR') return 'border-red-200 bg-red-50';
    if (status === 'PROCESSING') return 'border-blue-200 bg-blue-50';
    return 'border-gray-200 bg-gray-50';
  };

  // Helper function for status messages
  const getStatusMessage = (status: string) => {
    switch (status) {
      case 'PROCESSING':
        return 'AI is analyzing this document...';
      case 'ERROR':
        return 'Processing failed. Please try uploading again.';
      default:
        return 'Document uploaded but summary processing hasn\'t started yet.';
    }
  };

  // Consolidated document categorization
  const categorizedSummaries = summaries.reduce((acc, summary) => {
    if (summary.summaryStatus === 'PROCESSED') {
      acc.ready.push(summary);
    } else if (summary.summaryStatus === 'PROCESSING') {
      acc.processing.push(summary);
    } else {
      acc.pending.push(summary);
    }
    return acc;
  }, { ready: [] as DocumentSummary[], processing: [] as DocumentSummary[], pending: [] as DocumentSummary[] });

  // Fetch document summaries
  const fetchSummaries = async () => {
    if (documentIds.length === 0) {
      setSummaries([]);
      return;
    }
    
    setLoading(true);
    try {
      const summaryPromises = documentIds.map(async (documentId) => {
        try {
          const content = await apiService.getDocumentContent(documentId);
          const documents = await apiService.listDocuments();
          const document = documents.documents.find(doc => doc.id === documentId);
          
          return {
            id: documentId,
            name: document?.name || 'Unknown Document',
            summary: content.summary,
            status: document?.status || 'UNKNOWN',
            summaryStatus: content.summaryStatus || 'UNKNOWN',
            uploadDate: document?.uploadDate || '',
            readTime: content.summary ? `${Math.ceil(content.summary.split(' ').length / 200)} min read` : undefined,
            keyPoints: content.summary ? extractKeyPoints(content.summary) : undefined
          };
        } catch (error) {
          console.error(`Error fetching summary for document ${documentId}:`, error);
          return {
            id: documentId,
            name: 'Unknown Document',
            summary: null,
            status: 'ERROR',
            summaryStatus: 'ERROR',
            uploadDate: '',
            error: error instanceof Error ? error.message : 'Failed to fetch summary'
          };
        }
      });

      const results = await Promise.all(summaryPromises);
      setSummaries(results);
    } catch (error) {
      console.error('Error fetching summaries:', error);
      toast({
        title: 'Error fetching summaries',
        description: error instanceof Error ? error.message : 'Failed to load document summaries',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // Extract key points from summary text
  const extractKeyPoints = (summary: string): string[] => {
    // Simple extraction: look for bullet points, numbered lists, or key phrases
    const sentences = summary.split(/[.!?]+/).filter(s => s.trim().length > 20);
    const keyPoints = sentences.slice(0, 4).map(sentence => 
      sentence.trim().replace(/^[#\s]*[-•*]?\s*/, '').substring(0, 100) + (sentence.length > 100 ? '...' : '')
    );
    return keyPoints;
  };

  // Refresh summaries
  const handleRefresh = async () => {
    setRefreshing(true);
    await fetchSummaries();
    setRefreshing(false);
    toast({
      title: 'Summaries refreshed',
      description: 'Document summaries have been updated.',
    });
  };

  // Export all summaries as PDFs
  const handleExportAllPDFs = () => {
    if (categorizedSummaries.ready.length === 0) {
      toast({
        title: 'No summaries to export',
        description: 'Please wait for summaries to be generated.',
        variant: 'destructive',
      });
      return;
    }
    
    categorizedSummaries.ready.forEach((summary, index) => {
      // Add a small delay between exports to avoid browser blocking
      setTimeout(() => {
        handleExportPDF(summary);
      }, index * 500);
    });
    
    toast({
      title: 'Exporting all summaries',
      description: `${categorizedSummaries.ready.length} PDF(s) will be downloaded.`,
    });
  };

  // Export summary as PDF
  const handleExportPDF = (summary: DocumentSummary) => {
    try {
      const doc = new jsPDF();
      
      // Set font and size
      doc.setFont('helvetica');
      doc.setFontSize(20);
      
      // Title
      doc.text('StudyMate Summary', 20, 20);
      
      // Document name
      doc.setFontSize(16);
      doc.text(`Document: ${summary.name}`, 20, 35);
      
      // Date
      doc.setFontSize(12);
      const currentDate = new Date().toLocaleDateString();
      doc.text(`Generated on: ${currentDate}`, 20, 45);
      
      // Summary content
      doc.setFontSize(14);
      doc.text('Summary:', 20, 60);
      
      doc.setFontSize(12);
      const summaryText = summary.summary || 'No summary available';
      const splitText = doc.splitTextToSize(summaryText, 170); // 170 is the width
      doc.text(splitText, 20, 70);
      
      // Key points
      if (summary.keyPoints && summary.keyPoints.length > 0) {
        const startY = 70 + (splitText.length * 7) + 20; // Position after summary
        doc.setFontSize(14);
        doc.text('Key Points:', 20, startY);
        
        doc.setFontSize(12);
        summary.keyPoints.forEach((point, index) => {
          const yPos = startY + 15 + (index * 10);
          if (yPos < 280) { // Check if we have space on the page
            doc.text(`• ${point}`, 25, yPos);
          }
        });
      }
      
      // Footer
      doc.setFontSize(10);
      doc.text('Generated by StudyMate AI', 20, 290);
      
      // Save the PDF
      const fileName = `${summary.name.replace(/[^a-zA-Z0-9]/g, '_')}_summary.pdf`;
      doc.save(fileName);
      
      toast({
        title: 'PDF exported successfully!',
        description: `Summary for "${summary.name}" has been downloaded.`,
      });
    } catch (error) {
      console.error('Error generating PDF:', error);
      toast({
        title: 'Export failed',
        description: 'Failed to generate PDF. Please try again.',
        variant: 'destructive',
      });
    }
  };

  // Generate quiz from summary (placeholder)
  const handleGenerateQuiz = (summary: DocumentSummary) => {
    toast({
      title: 'Quiz generation coming soon',
      description: 'Quiz generation from summaries will be available in the next update.',
    });
  };

  // Create flashcards from summary (placeholder)
  const handleCreateFlashcards = (summary: DocumentSummary) => {
    toast({
      title: 'Flashcard creation coming soon',
      description: 'Flashcard creation from summaries will be available in the next update.',
    });
  };

  // Fetch summaries on component mount and when documentIds change
  useEffect(() => {
    fetchSummaries();
  }, [documentIds]);

  // Poll for updates when there are documents being processed
  useEffect(() => {
    const documentsBeingProcessed = summaries.filter(s => s.summaryStatus === 'PROCESSING');
    
    if (documentsBeingProcessed.length > 0) {
      const interval = setInterval(() => {
        fetchSummaries();
      }, 30000); // Poll every 30 seconds
      
      return () => clearInterval(interval);
    }
  }, [summaries, documentIds]);

  if (uploadedFiles.length === 0) {
    return (
      <Card className="text-center py-12">
        <CardContent>
          <FileText className="h-16 w-16 text-gray-400 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-gray-900 mb-2">No files uploaded yet</h3>
          <p className="text-gray-600 mb-6">Upload your course materials to see AI-generated summaries</p>
          <Button variant="outline">Go to Upload Tab</Button>
        </CardContent>
      </Card>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-center">
          <Loader2 className="h-12 w-12 text-blue-500 animate-spin mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-gray-900 mb-2">Loading summaries...</h3>
          <p className="text-gray-600">AI is analyzing your documents and generating summaries</p>
        </div>
      </div>
    );
  }

  const { ready, processing, pending } = categorizedSummaries;
  

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">AI-Generated Summaries</h2>
          <p className="text-gray-600">Clear, concise summaries of your course materials</p>
        </div>
        <div className="flex space-x-2">
          <Button 
            variant="outline" 
            onClick={handleRefresh}
            disabled={refreshing}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
            Refresh
          </Button>
          <Button 
            className="bg-purple-600 hover:bg-purple-700"
            onClick={() => handleExportAllPDFs()}
            disabled={ready.length === 0}
          >
            <Download className="h-4 w-4 mr-2" />
            Export All
          </Button>
        </div>
      </div>

      {/* Documents with summaries */}
      {ready.length > 0 && (
        <div className="grid gap-6">
          {ready.map((item) => (
            <Card key={item.id} className="hover:shadow-lg transition-shadow">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <CardTitle className="text-xl text-gray-900 mb-2">{item.name}</CardTitle>
                    <CardDescription className="flex items-center space-x-4 text-sm">
                      <span className="flex items-center">
                        <FileText className="h-4 w-4 mr-1" />
                        {item.name}
                      </span>
                      {item.readTime && (
                        <span className="flex items-center">
                          <Clock className="h-4 w-4 mr-1" />
                          {item.readTime}
                        </span>
                      )}
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusBadgeStyle(item.summaryStatus)}`}>
                        {item.summaryStatus}
                      </span>
                    </CardDescription>
                  </div>
                  <div className="flex items-center space-x-1 bg-purple-100 px-2 py-1 rounded-full">
                    <Sparkles className="h-3 w-3 text-purple-600" />
                    <span className="text-xs font-medium text-purple-600">AI Generated</span>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-6">
                {/* Summary Text */}
                <div>
                  <h4 className="font-semibold text-gray-900 mb-2">Summary</h4>
                  <div className="prose prose-sm max-w-none text-gray-700 leading-relaxed">
                    <ReactMarkdown>
                      {item.summary || 'Summary is ready but content is not available. Please refresh or try again.'}
                    </ReactMarkdown>
                  </div>
                </div>

                {/* Key Points */}
                {item.keyPoints && item.keyPoints.length > 0 && (
                  <div>
                    <h4 className="font-semibold text-gray-900 mb-3 flex items-center">
                      <BookOpen className="h-4 w-4 mr-2" />
                      Key Points
                    </h4>
                    <ul className="space-y-2">
                      {item.keyPoints.map((point, pointIndex) => (
                        <li key={pointIndex} className="flex items-start">
                          <div className="w-2 h-2 bg-purple-500 rounded-full mt-2 mr-3 flex-shrink-0"></div>
                          <ReactMarkdown className="text-gray-700">{point}</ReactMarkdown>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Actions */}
                <div className="flex space-x-3 pt-4 border-t border-gray-100">
                  <Button 
                    variant="outline" 
                    size="sm"
                    onClick={() => handleExportPDF(item)}
                  >
                    <Download className="h-4 w-4 mr-2" />
                    Export PDF
                  </Button>
                  <Button 
                    variant="outline" 
                    size="sm"
                    onClick={() => handleGenerateQuiz(item)}
                  >
                    Generate Quiz
                  </Button>
                  <Button 
                    variant="outline" 
                    size="sm"
                    onClick={() => handleCreateFlashcards(item)}
                  >
                    Create Flashcards
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Documents being processed */}
      {processing.length > 0 && (
        <div className="space-y-4">
          <h3 className="text-lg font-semibold text-gray-900">Documents Being Processed</h3>
          <div className="grid gap-4">
            {processing.map((item) => (
              <Card key={item.id} className={getCardStyle(item.summaryStatus)}>
                <CardContent className="pt-6">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <Loader2 className="h-5 w-5 text-blue-600 animate-spin" />
                      <div>
                        <h4 className="font-medium text-gray-900">{item.name}</h4>
                        <p className="text-sm text-gray-600">
                          {getStatusMessage(item.summaryStatus)}
                        </p>
                      </div>
                    </div>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusBadgeStyle(item.summaryStatus)}`}>
                      {item.summaryStatus}
                    </span>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* Documents not yet processed */}
      {pending.length > 0 && (
        <div className="space-y-4">
          <h3 className="text-lg font-semibold text-gray-900">
            {pending.some(d => d.summaryStatus === 'ERROR') ? 'Documents with Issues' : 'Documents Pending Processing'}
          </h3>
          <div className="grid gap-4">
            {pending.map((item) => (
              <Card key={item.id} className={getCardStyle(item.summaryStatus)}>
                <CardContent className="pt-6">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <FileText className={`h-5 w-5 ${getStatusBadgeStyle(item.summaryStatus)}`} />
                      <div>
                        <h4 className="font-medium text-gray-900">{item.name}</h4>
                        <p className="text-sm text-gray-600">
                          {getStatusMessage(item.summaryStatus)}
                        </p>
                      </div>
                    </div>
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusBadgeStyle(item.summaryStatus)}`}>
                      {item.summaryStatus === 'ERROR' ? 'ERROR' : 'PENDING'}
                    </span>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* No documents at all */}
      {summaries.length === 0 && !loading && (
        <Card className="text-center py-12">
          <CardContent>
            <FileText className="h-16 w-16 text-gray-400 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-gray-900 mb-2">No documents found</h3>
            <p className="text-gray-600 mb-6">Upload your course materials to generate AI summaries!</p>
            <Button variant="outline" onClick={handleRefresh}>
              <RefreshCw className="h-4 w-4 mr-2" />
              Refresh
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default SummaryTab;
