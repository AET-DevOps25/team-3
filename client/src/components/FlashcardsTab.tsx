
import { useState, useEffect, useRef } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { BookOpen, RotateCcw, ChevronLeft, ChevronRight, Shuffle, Eye, EyeOff, Loader2 } from 'lucide-react';
import { apiService, FlashcardModel, DocumentStatus } from '../lib/api';

interface FlashcardsTabProps {
  uploadedFiles: File[];
  documentIds: string[];
}

interface FlashcardDeck {
  id: string;
  title: string;
  description: string;
  cardCount: number;
  source: string;
  difficulty: string;
  status?: DocumentStatus | 'PENDING';
  error?: string;
  flashcards: FlashcardModel[];
}

const FlashcardsTab = ({ uploadedFiles, documentIds }: FlashcardsTabProps) => {
  const [selectedDeck, setSelectedDeck] = useState<number | null>(null);
  const [currentCard, setCurrentCard] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [flashcardDecks, setFlashcardDecks] = useState<FlashcardDeck[]>([]);
  const fetchedDocumentIds = useRef<Set<string>>(new Set());
  const processingDocumentIds = useRef<Set<string>>(new Set());

  useEffect(() => {
    const newDocumentIds = documentIds.filter(id => !fetchedDocumentIds.current.has(id));
    if (newDocumentIds.length === 0) return;
    
    // Mark as being processed to avoid duplicate processing
    newDocumentIds.forEach(id => processingDocumentIds.current.add(id));
    
    // First, add pending entries for new documents
    const pendingEntries = newDocumentIds.map(documentId => ({
      id: documentId,
      title: 'Loading...',
      description: 'Preparing flashcards...',
      cardCount: 0,
      source: 'Loading...',
      difficulty: 'Unknown',
      status: 'PENDING' as const,
      error: undefined,
      flashcards: []
    }));
    
    setFlashcardDecks(prevDecks => [...prevDecks, ...pendingEntries]);
    
    const fetchFlashcards = async () => {
      const flashcardPromises = newDocumentIds.map(async (documentId) => {
        try {
          // Check if flashcard data is already available from automatic processing
          const documentContent = await apiService.getDocumentContent(documentId);
          
          // If flashcards are ready and have data, use them directly
          if (documentContent.flashcardStatus === 'READY' && documentContent.flashcardData) {
            try {
              const flashcardDataMap = documentContent.flashcardData.response || documentContent.flashcardData;
              const flashcardsList = flashcardDataMap.flashcards || [];
              
              if (flashcardsList.length > 0) {
                const flashcards = flashcardsList.map(fc => ({
                  question: fc.question || "",
                  answer: fc.answer || "",
                  difficulty: fc.difficulty || "medium"
                }));
                
                return {
                  id: documentId,
                  title: documentContent.originalName || 'Unknown Document',
                  description: `${flashcards.length} flashcards generated from your document`,
                  cardCount: flashcards.length,
                  source: documentContent.originalName || 'Unknown Document',
                  difficulty: flashcards.length > 10 ? 'Advanced' : 'Beginner',
                  status: 'READY',
                  error: undefined,
                  flashcards
                };
              }
            } catch (parseError) {
              console.warn('Failed to parse existing flashcard data:', parseError);
            }
          }
          
          // Check if we should wait for automatic processing or make individual API call
          const shouldWaitForAutoProcessing = 
            documentContent.status === 'PROCESSING' || 
            documentContent.summaryStatus === 'PROCESSING' ||
            documentContent.quizStatus === 'PROCESSING' ||
            documentContent.flashcardStatus === 'PROCESSING' ||
            documentContent.summaryStatus === 'UPLOADED' || 
            documentContent.flashcardStatus === 'UPLOADED';

          if (shouldWaitForAutoProcessing) {
            return {
              id: documentId,
              title: documentContent.originalName || 'Unknown Document',
              description: 'Flashcards are being generated automatically...',
              cardCount: 0,
              source: documentContent.originalName || 'Unknown Document',
              difficulty: 'Unknown',
              status: 'PROCESSING',
              error: 'Flashcards are being generated automatically...',
              flashcards: []
            };
          }
          
          // Only make individual API call if automatic processing failed
          if (documentContent.flashcardStatus === 'ERROR') {
            const res = await apiService.getFlashcardsForDocument(documentId);
            
            if (res.status === 'PROCESSING') {
              pollFlashcardStatus(documentId);
            }
            
            return {
              id: documentId,
              title: res.documentName || 'Unknown Document',
              description: res.status === 'READY' ? `${res.flashcards.length} flashcards generated from your document` : 'Failed to generate flashcards',
              cardCount: res.flashcards.length,
              source: res.documentName || 'Unknown Document',
              difficulty: res.flashcards.length > 10 ? 'Advanced' : 'Beginner',
              status: res.status,
              error: res.error,
              flashcards: res.flashcards
            };
          }
          
          // Default: wait for automatic processing
          return {
            id: documentId,
            title: documentContent.originalName || 'Unknown Document',
            description: 'Waiting for automatic flashcard generation...',
            cardCount: 0,
            source: documentContent.originalName || 'Unknown Document',
            difficulty: 'Unknown',
            status: 'PROCESSING',
            error: 'Waiting for automatic flashcard generation...',
            flashcards: []
          };
          
        } catch (error) {
          return {
            id: documentId,
            title: 'Unknown Document',
            description: 'Failed to load flashcards',
            cardCount: 0,
            source: 'Unknown Document',
            difficulty: 'Unknown',
            status: 'ERROR',
            error: error instanceof Error ? error.message : 'Failed to fetch flashcards',
            flashcards: []
          };
        }
      });
      
      const results = await Promise.all(flashcardPromises);
      
      // Mark as fetched and remove from processing
      newDocumentIds.forEach(id => {
        fetchedDocumentIds.current.add(id);
        processingDocumentIds.current.delete(id);
      });
      
      setFlashcardDecks(prevDecks => {
        const existingDecks = prevDecks.filter(d => !newDocumentIds.includes(d.id));
        const mappedResults = results.map(result => ({
          id: result.id,
          title: result.title,
          description: result.description,
          cardCount: result.cardCount,
          source: result.source,
          difficulty: result.difficulty,
                      status: result.status as 'PROCESSING' | 'READY' | 'ERROR' | 'PENDING',
          error: result.error,
          flashcards: result.flashcards
        }));
        return [...existingDecks, ...mappedResults];
      });
    };
    
    fetchFlashcards();
  }, [documentIds.join(",")]);  // Keep dependencies minimal to avoid infinite loops

  const pollFlashcardStatus = async (documentId: string) => {
    const maxAttempts = 30;
    let attempts = 0;
    
    const poll = async () => {
      try {
        const res = await apiService.getFlashcardsForDocument(documentId);
        
        if (res.status === 'READY' || res.status === 'ERROR') {
          setFlashcardDecks(prevDecks => {
            const updatedDecks = prevDecks.map(d => 
              d.id === documentId ? {
                ...d,
                title: res.documentName || d.title,
                description: res.status === 'READY' ? `${res.flashcards.length} flashcards generated from your document` : 'Failed to generate flashcards',
                cardCount: res.flashcards.length,
                source: res.documentName || d.source,
                difficulty: res.flashcards.length > 10 ? 'Advanced' : 'Beginner',
                status: res.status,
                error: res.error,
                flashcards: res.flashcards
              } : d
            );
            return updatedDecks;
          });
          return;
        }
        
        attempts++;
        if (attempts < maxAttempts) {
          setTimeout(poll, 30000);
        } else {
          setFlashcardDecks(prevDecks => {
            const updatedDecks = prevDecks.map(d => 
              d.id === documentId ? {
                ...d,
                status: 'ERROR' as const,
                error: 'Flashcard generation timed out. Please try again.'
              } : d
            );
            return updatedDecks;
          });
        }
      } catch (error) {
        attempts++;
        if (attempts < maxAttempts) {
          setTimeout(poll, 30000);
        }
      }
    };
    
    poll();
  };

  const nextCard = () => {
    if (selectedDeck === null) return;
    const deck = flashcardDecks[selectedDeck];
    setCurrentCard((prev) => (prev + 1) % deck.flashcards.length);
    setIsFlipped(false);
  };

  const prevCard = () => {
    if (selectedDeck === null) return;
    const deck = flashcardDecks[selectedDeck];
    setCurrentCard((prev) => (prev - 1 + deck.flashcards.length) % deck.flashcards.length);
    setIsFlipped(false);
  };

  const shuffleDeck = () => {
    if (selectedDeck === null) return;
    const deck = flashcardDecks[selectedDeck];
    setCurrentCard(Math.floor(Math.random() * deck.flashcards.length));
    setIsFlipped(false);
  };

  const readyFlashcards = flashcardDecks.filter(deck => deck.status === 'READY' && deck.flashcards && deck.flashcards.length > 0);
  const generatingFlashcards = flashcardDecks.filter(deck => deck.status === 'PROCESSING');
  const failedFlashcards = flashcardDecks.filter(deck => deck.status === 'ERROR');
  const pendingFlashcards = flashcardDecks.filter(deck => 
    !deck.status || deck.status === 'PENDING' || 
    (deck.status !== 'READY' && deck.status !== 'PROCESSING' && deck.status !== 'ERROR')
  );

  return (
    <>
      {/* Flashcard study view */}
      {selectedDeck !== null && (() => {
        const deck = flashcardDecks[selectedDeck];
        if (!deck || deck.flashcards.length === 0) {
    return (
            <div className="max-w-4xl mx-auto text-center py-12">
              <h2 className="text-2xl font-bold text-gray-900 mb-4">No flashcards available</h2>
              <p className="text-gray-600 mb-6">This deck doesn't have any flashcards yet.</p>
              <Button onClick={() => setSelectedDeck(null)}>Back to Decks</Button>
            </div>
    );
  }

        const currentFlashcard = deck.flashcards[currentCard];
        
    return (
      <div className="max-w-4xl mx-auto space-y-6">
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                    <CardTitle className="text-xl">{deck.title}</CardTitle>
                <CardDescription>
                      Card {currentCard + 1} of {deck.flashcards.length}
                </CardDescription>
              </div>
              <div className="flex items-center space-x-2">
                <Button variant="outline" size="sm" onClick={shuffleDeck}>
                  <Shuffle className="h-4 w-4" />
                </Button>
                <Button variant="outline" onClick={() => setSelectedDeck(null)}>
                  Exit Study
                </Button>
              </div>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div 
                className="bg-purple-600 h-2 rounded-full transition-all duration-300"
                    style={{ width: `${((currentCard + 1) / deck.flashcards.length) * 100}%` }}
              ></div>
            </div>
          </CardHeader>
        </Card>

        <div className="flex justify-center">
          <div 
            className="relative w-full max-w-2xl h-80 cursor-pointer"
            onClick={() => setIsFlipped(!isFlipped)}
          >
            <div className={`absolute inset-0 w-full h-full transition-transform duration-600 transform-style-preserve-3d ${isFlipped ? 'rotate-y-180' : ''}`}>
              <Card className="absolute inset-0 w-full h-full backface-hidden border-2 border-purple-200 shadow-lg hover:shadow-xl transition-shadow">
                <CardContent className="h-full flex flex-col items-center justify-center p-8 text-center">
                  <div className="mb-4">
                    <Eye className="h-8 w-8 text-purple-500 mx-auto" />
                  </div>
                  <h3 className="text-2xl font-bold text-gray-900 mb-4">
                        {currentFlashcard.question}
                  </h3>
                  <p className="text-gray-600">Click to reveal answer</p>
                </CardContent>
              </Card>

              <Card className="absolute inset-0 w-full h-full backface-hidden rotate-y-180 border-2 border-green-200 shadow-lg bg-green-50">
                <CardContent className="h-full flex flex-col items-center justify-center p-8 text-center">
                  <div className="mb-4">
                    <EyeOff className="h-8 w-8 text-green-500 mx-auto" />
                  </div>
                  <p className="text-lg text-gray-800 leading-relaxed">
                        {currentFlashcard.answer}
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>

        <div className="flex justify-center space-x-4">
          <Button variant="outline" onClick={prevCard}>
            <ChevronLeft className="h-4 w-4 mr-2" />
            Previous
          </Button>
          <Button 
            variant="outline" 
            onClick={() => setIsFlipped(!isFlipped)}
            className="px-8"
          >
            <RotateCcw className="h-4 w-4 mr-2" />
            Flip Card
          </Button>
          <Button variant="outline" onClick={nextCard}>
            Next
            <ChevronRight className="h-4 w-4 ml-2" />
          </Button>
        </div>

        <Card>
          <CardContent className="pt-6">
            <div className="flex justify-center space-x-4">
              <Button variant="outline" className="text-red-600 border-red-200 hover:bg-red-50">
                Hard
              </Button>
              <Button variant="outline" className="text-yellow-600 border-yellow-200 hover:bg-yellow-50">
                Medium
              </Button>
              <Button variant="outline" className="text-green-600 border-green-200 hover:bg-green-50">
                Easy
              </Button>
            </div>
            <p className="text-center text-sm text-gray-600 mt-3">
              Rate your confidence to improve future study sessions
            </p>
          </CardContent>
        </Card>
      </div>
    );
      })()}

      {/* Empty state */}
      {uploadedFiles.length === 0 || documentIds.length === 0 ? (
        <Card className="text-center py-12">
          <CardContent>
            <BookOpen className="h-16 w-16 text-gray-400 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-gray-900 mb-2">No flashcards available</h3>
            <p className="text-gray-600 mb-6">Upload your course materials to generate flashcards</p>
            <Button variant="outline">Go to Upload Tab</Button>
          </CardContent>
        </Card>
      ) : (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
              <h2 className="text-2xl font-bold text-gray-900">AI-Generated Flashcards</h2>
              <p className="text-gray-600">Study with personalized flashcards</p>
        </div>
      </div>

          {readyFlashcards.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-gray-900">Available Flashcard Decks</h3>
      <div className="grid gap-6 md:grid-cols-2">
                {readyFlashcards.map((deck, index) => (
                  <Card key={deck.id} className="hover:shadow-lg transition-shadow cursor-pointer group" onClick={() => setSelectedDeck(index)}>
            <CardHeader>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <CardTitle className="text-lg text-gray-900 group-hover:text-purple-600 transition-colors">
                    {deck.title}
                  </CardTitle>
                          <CardDescription className="text-sm text-gray-600">
                            {deck.description}
                          </CardDescription>
                </div>
                        <div className="flex items-center space-x-2">
                          <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                            deck.difficulty === 'Easy' ? 'bg-green-100 text-green-600' :
                            deck.difficulty === 'Medium' ? 'bg-yellow-100 text-yellow-600' :
                            'bg-red-100 text-red-600'
                          }`}>
                  {deck.difficulty}
                </span>
              </div>
                      </div>
                    </CardHeader>
                    <CardContent>
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-2 text-sm text-gray-600">
                          <BookOpen className="h-4 w-4" />
                          <span>{deck.cardCount} cards</span>
              </div>
                        <Button size="sm" className="bg-purple-600 hover:bg-purple-700">
                          Start Studying
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
            </div>
          )}

          {generatingFlashcards.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-gray-900">Flashcard Decks Being Generated</h3>
              <div className="grid gap-6 md:grid-cols-2">
                {generatingFlashcards.map((deck) => (
                  <Card key={deck.id} className="border-yellow-200 bg-yellow-50">
                    <CardContent className="pt-6">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-3">
                          <Loader2 className="h-5 w-5 text-yellow-600 animate-spin" />
            <div>
                            <h4 className="font-medium text-gray-900">{deck.title}</h4>
                            <p className="text-sm text-gray-600">AI is generating your flashcards...</p>
                          </div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {failedFlashcards.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-gray-900">Failed Flashcard Decks</h3>
              <div className="grid gap-6 md:grid-cols-2">
                {failedFlashcards.map((deck) => (
                  <Card key={deck.id} className="border-red-200 bg-red-50">
                    <CardContent className="pt-6">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-3">
            <div>
                            <h4 className="font-medium text-gray-900">{deck.title}</h4>
                            <p className="text-sm text-red-600">{deck.error || 'Failed to generate flashcards'}</p>
                          </div>
            </div>
          </div>
        </CardContent>
      </Card>
                ))}
              </div>
            </div>
          )}

          {pendingFlashcards.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-gray-900">Pending Flashcard Decks</h3>
              <div className="grid gap-6 md:grid-cols-2">
                {pendingFlashcards.map((deck) => (
                  <Card key={deck.id} className="border-gray-200 bg-gray-50">
                    <CardContent className="pt-6">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-3">
                          <Loader2 className="h-5 w-5 text-gray-600 animate-spin" />
                          <div>
                            <h4 className="font-medium text-gray-900">{deck.title}</h4>
                            <p className="text-sm text-gray-600">Flashcard generation hasn't started yet</p>
                          </div>
                        </div>
                        <span className="px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
                          PENDING
                        </span>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {readyFlashcards.length === 0 && generatingFlashcards.length === 0 && failedFlashcards.length === 0 && pendingFlashcards.length === 0 && (
            <Card className="text-center py-12">
              <CardContent>
                <BookOpen className="h-16 w-16 text-gray-400 mx-auto mb-4" />
                <h3 className="text-xl font-semibold text-gray-900 mb-2">No flashcards available</h3>
                <p className="text-gray-600 mb-6">Upload your course materials to generate flashcards</p>
                <Button variant="outline">Go to Upload Tab</Button>
              </CardContent>
            </Card>
          )}
    </div>
      )}
    </>
  );
};

export default FlashcardsTab;
