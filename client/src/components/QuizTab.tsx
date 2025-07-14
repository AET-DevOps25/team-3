import { useEffect, useState, useRef } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Brain, CheckCircle, XCircle, Trophy, RotateCcw, Loader2, FileText } from 'lucide-react';
import { apiService, DocumentStatus } from '../lib/api';

interface QuizTabProps {
  uploadedFiles: File[];
  documentIds: string[];
  quizzes: QuizData[];
  setQuizzes: React.Dispatch<React.SetStateAction<QuizData[]>>;
  answers: { [documentId: string]: { [questionIndex: number]: string | number } };
  setAnswers: React.Dispatch<React.SetStateAction<{ [documentId: string]: { [questionIndex: number]: string | number } }>>;
}

interface QuizQuestion {
  type: string;
  question: string;
  correctAnswer: string;
  points: number;
  options?: string[];
}

interface QuizData {
  questions: QuizQuestion[];
  documentName: string;
  documentId: string;
  status?: DocumentStatus | 'PENDING';
  error?: string;
}

const QuizTab = ({ uploadedFiles, documentIds, quizzes, setQuizzes, answers, setAnswers }: QuizTabProps) => {
  const [selectedQuiz, setSelectedQuiz] = useState<number | null>(null);
  const [currentQuestion, setCurrentQuestion] = useState(0);
  const [showResults, setShowResults] = useState(false);
  const fetchedDocumentIds = useRef<Set<string>>(new Set());

  const getCurrentAnswers = (quizId: string) => answers[quizId] || {};
  const setCurrentAnswers = (quizId: string, newAnswers: { [questionIndex: number]: string | number }) => {
    setAnswers(prev => ({ ...prev, [quizId]: newAnswers }));
  };

  useEffect(() => {
    const newDocumentIds = documentIds.filter(id => !fetchedDocumentIds.current.has(id));
    if (newDocumentIds.length === 0) return;
    
    // First, add pending entries for new documents
    const pendingEntries = newDocumentIds.map(documentId => ({
      questions: [],
      documentName: 'Loading...',
      documentId,
      status: 'PENDING' as const,
      error: undefined
    }));
    
    setQuizzes(prevQuizzes => [...prevQuizzes, ...pendingEntries]);
    
    const fetchQuizzes = async () => {
      const quizPromises = newDocumentIds.map(async (documentId) => {
        try {
          // Check if quiz data is already available from automatic processing
          const documentContent = await apiService.getDocumentContent(documentId);
          
          // If quiz is ready and has data, use it directly
          if ((documentContent.quizStatus === 'PROCESSED') && documentContent.quizData) {
            try {
              let questionsData = [];
              
              if (Array.isArray(documentContent.quizData.questions)) {
                questionsData = documentContent.quizData.questions;
              } else if (documentContent.quizData.response && Array.isArray(documentContent.quizData.response.questions)) {
                questionsData = documentContent.quizData.response.questions;
              } else if (documentContent.quizData.questions && Array.isArray(documentContent.quizData.questions)) {
                questionsData = documentContent.quizData.questions;
              }
              
              if (questionsData.length > 0) {
                return {
                  documentId,
                  title: documentContent.originalName || 'Unknown Document',
                  description: `${questionsData.length} questions generated from your document`,
                  questions: questionsData,
                  status: 'PROCESSED',
                  error: undefined
                };
              }
            } catch (parseError) {
              console.warn('Failed to parse existing quiz data:', parseError);
            }
          }
          
          // Check if we should wait for automatic processing or make individual API call
          const shouldWaitForAutoProcessing = 
            documentContent.status === 'PROCESSING' || 
            documentContent.summaryStatus === 'PROCESSING' ||
            documentContent.quizStatus === 'PROCESSING' ||
            documentContent.flashcardStatus === 'PROCESSING' ||
            documentContent.summaryStatus === 'UPLOADED' || 
            documentContent.quizStatus === 'UPLOADED';

          if (shouldWaitForAutoProcessing) {
            return {
              questions: [],
              documentName: documentContent.originalName || 'Unknown Document',
              documentId,
              status: 'PROCESSING',
              error: 'Quiz is being generated automatically...'
            };
          }
          
          // Only make individual API call if automatic processing failed
          if (documentContent.quizStatus === 'ERROR') {
            const res = await apiService.getQuizForDocument(documentId);
            
            if (res.status === 'PROCESSING') {
              pollQuizStatus(documentId);
            }
            
            return res;
          }
          
          // Default: wait for automatic processing
          return {
            questions: [],
            documentName: documentContent.originalName || 'Unknown Document',
            documentId,
            status: 'PROCESSING',
            error: 'Waiting for automatic quiz generation...'
          };
          
        } catch (error) {
          return {
            questions: [],
            documentName: 'Unknown Document',
            documentId,
            status: 'ERROR',
            error: error instanceof Error ? error.message : 'Failed to fetch quiz',
          };
        }
      });
      
      const results = await Promise.all(quizPromises);
      newDocumentIds.forEach(id => fetchedDocumentIds.current.add(id));
      
      setQuizzes(prevQuizzes => {
        const existingQuizzes = prevQuizzes.filter(q => !newDocumentIds.includes(q.documentId));
        const mappedResults = results.map(result => ({
          documentId: result.documentId,
          title: result.title,
          description: result.description,
          questions: result.questions,
          status: result.status as 'PROCESSING' | 'PROCESSED' | 'ERROR' | 'PENDING',
          error: result.error
        }));
        return [...existingQuizzes, ...mappedResults];
      });
    };
    
    fetchQuizzes();
  }, [documentIds.join(",")]);  // Removed 'quizzes' and 'setQuizzes' from dependencies

  // Poll for updates when there are documents being processed
  useEffect(() => {
    const documentsBeingProcessed = quizzes.filter(q => q.status === 'PROCESSING' || q.status === 'PENDING');
    
    if (documentsBeingProcessed.length > 0) {
      const interval = setInterval(() => {
        // Re-fetch document content for processing documents
        documentsBeingProcessed.forEach(async (quiz) => {
          try {
            const documentContent = await apiService.getDocumentContent(quiz.documentId);
                         if (documentContent.quizStatus === 'PROCESSED' && documentContent.quizData) {
              setQuizzes(prevQuizzes => 
                prevQuizzes.map(q => {
                  if (q.documentId === quiz.documentId) {
                    // Parse quiz data
                    let questionsData = [];
                    
                    if (Array.isArray(documentContent.quizData.questions)) {
                      questionsData = documentContent.quizData.questions;
                    } else if (documentContent.quizData.response && Array.isArray(documentContent.quizData.response.questions)) {
                      questionsData = documentContent.quizData.response.questions;
                    } else if (documentContent.quizData.questions && Array.isArray(documentContent.quizData.questions)) {
                      questionsData = documentContent.quizData.questions;
                    }
                    
                    if (questionsData.length > 0) {
                      return {
                        ...q,
                        title: documentContent.originalName || 'Unknown Document',
                        description: `${questionsData.length} questions generated from your document`,
                        questions: questionsData,
                        status: 'PROCESSED',
                        error: undefined
                      };
                    }
                  }
                  return q;
                })
              );
            }
          } catch (error) {
            console.error('Error polling quiz status:', error);
          }
        });
      }, 15000); // Poll every 15 seconds
      
      return () => clearInterval(interval);
    }
  }, [quizzes]);

  const pollQuizStatus = async (documentId: string) => {
    const maxAttempts = 30;
    let attempts = 0;
    
    const poll = async () => {
      try {
        const res = await apiService.getQuizForDocument(documentId);
        
        if (res.status === 'PROCESSED' || res.status === 'ERROR') {
          setQuizzes(prevQuizzes => {
            const updatedQuizzes = prevQuizzes.map(q => 
              q.documentId === documentId ? res : q
            );
            return updatedQuizzes;
          });
          return;
        }
        
        attempts++;
        if (attempts < maxAttempts) {
          setTimeout(poll, 30000);
        } else {
          setQuizzes(prevQuizzes => {
            const updatedQuizzes = prevQuizzes.map(q => 
              q.documentId === documentId ? {
                ...q,
                status: 'ERROR' as const,
                error: 'Quiz generation timed out. Please try again.'
              } : q
            );
            return updatedQuizzes;
          });
        }
      } catch (error) {
        attempts++;
        if (attempts < maxAttempts) {
          setTimeout(poll, 30000);
        }
      }
    };
    
    setTimeout(poll, 5000);
  };

  const handleAnswerSelect = (questionIndex: number, answerIndex: number) => {
    if (selectedQuiz === null) return;
    const quiz = quizzes[selectedQuiz];
    const quizId = quiz.documentId;
    const prevAnswers = getCurrentAnswers(quizId);
    setCurrentAnswers(quizId, { ...prevAnswers, [questionIndex]: answerIndex });
  };

  const handleShortAnswerChange = (questionIndex: number, value: string) => {
    if (selectedQuiz === null) return;
    const quiz = quizzes[selectedQuiz];
    const quizId = quiz.documentId;
    const prevAnswers = getCurrentAnswers(quizId);
    setCurrentAnswers(quizId, { ...prevAnswers, [questionIndex]: value });
  };

  const handleSubmitQuiz = () => {
    setShowResults(true);
  };

  const calculateScore = (quiz: QuizData) => {
    const quizId = quiz.documentId;
    const userAnswers = getCurrentAnswers(quizId);
    let correct = 0;
    
    quiz.questions.forEach((question, index) => {
      if (question.options) {
        if (
          userAnswers[index] !== undefined &&
          question.options[userAnswers[index]] === question.correctAnswer
        ) {
          correct++;
        }
      } else {
        const userAnswerStr = typeof userAnswers[index] === 'string' ? userAnswers[index] : '';
        const userAnswerTrimmed = userAnswerStr ? userAnswerStr.trim() : '';
        const correctAnswerTrimmed = question.correctAnswer ? question.correctAnswer.trim() : '';
        if (userAnswerTrimmed.toLowerCase() === correctAnswerTrimmed.toLowerCase()) {
          correct++;
        }
      }
    });
    
    return quiz.questions.length > 0 ? Math.round((correct / quiz.questions.length) * 100) : 0;
  };

  // Filter ready quizzes
  const readyQuizzes = quizzes.filter(q => q.status === 'PROCESSED' && q.questions && q.questions.length > 0);
  
  // Display pending documents or errors
  const pendingDocuments = quizzes.filter(q => 
    (q.status !== 'PROCESSED' && q.status !== 'PROCESSING' && q.status !== 'ERROR')
  );

  return (
    <>
      {/* Quiz taking view */}
      {selectedQuiz !== null && !showResults && (() => {
        const quiz = quizzes[selectedQuiz];
        if (!quiz) return null;
        const question = quiz.questions[currentQuestion];
        
        return (
          <div className="max-w-4xl mx-auto space-y-6">
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle className="text-xl">{quiz.documentName}</CardTitle>
                    <CardDescription>Question {currentQuestion + 1} of {quiz.questions.length}</CardDescription>
                  </div>
                  <Button variant="outline" onClick={() => setSelectedQuiz(null)}>
                    Exit Quiz
                  </Button>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div 
                    className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                    style={{ width: `${((currentQuestion + 1) / quiz.questions.length) * 100}%` }}
                  ></div>
                </div>
              </CardHeader>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-lg leading-relaxed">
                  {question.question}
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {question.options ? (
                  question.options.map((option, index) => (
                    <button
                      key={index}
                      onClick={() => handleAnswerSelect(currentQuestion, index)}
                      className={`w-full p-4 text-left rounded-lg border-2 transition-all duration-200 ${
                        getCurrentAnswers(quiz.documentId)[currentQuestion] === index
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-200 hover:border-blue-300 hover:bg-blue-50/50'
                      }`}
                    >
                      <div className="flex items-center space-x-3">
                        <div className={`w-6 h-6 rounded-full border-2 flex items-center justify-center ${
                          getCurrentAnswers(quiz.documentId)[currentQuestion] === index
                            ? 'border-blue-500 bg-blue-500'
                            : 'border-gray-300'
                        }`}>
                          {getCurrentAnswers(quiz.documentId)[currentQuestion] === index && (
                            <div className="w-3 h-3 bg-white rounded-full"></div>
                          )}
                        </div>
                        <span className="text-gray-900">{option}</span>
                      </div>
                    </button>
                  ))
                ) : (
                  <input
                    type="text"
                    className="w-full p-4 border rounded-lg"
                    placeholder="Type your answer here"
                    value={typeof getCurrentAnswers(quiz.documentId)[currentQuestion] === 'string' ? getCurrentAnswers(quiz.documentId)[currentQuestion] : ''}
                    onChange={e => handleShortAnswerChange(currentQuestion, e.target.value)}
                  />
                )}

                <div className="flex justify-between pt-6">
                  <Button 
                    variant="outline" 
                    onClick={() => setCurrentQuestion(prev => Math.max(0, prev - 1))}
                    disabled={currentQuestion === 0}
                  >
                    Previous
                  </Button>
                  {currentQuestion === quiz.questions.length - 1 ? (
                    <Button 
                      onClick={handleSubmitQuiz}
                      disabled={getCurrentAnswers(quiz.documentId)[currentQuestion] === undefined || (question.options === undefined && !getCurrentAnswers(quiz.documentId)[currentQuestion])}
                      className="bg-green-600 hover:bg-green-700"
                    >
                      Submit Quiz
                    </Button>
                  ) : (
                    <Button 
                      onClick={() => setCurrentQuestion(prev => Math.min(quiz.questions.length - 1, prev + 1))}
                      disabled={getCurrentAnswers(quiz.documentId)[currentQuestion] === undefined || (question.options === undefined && !getCurrentAnswers(quiz.documentId)[currentQuestion])}
                    >
                      Next
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        );
      })()}

      {/* Results view */}
      {showResults && selectedQuiz !== null && (() => {
        const quiz = quizzes[selectedQuiz];
        if (!quiz) return (
          <div className="max-w-4xl mx-auto text-center py-12">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Quiz not found</h2>
            <p className="text-gray-600 mb-6">Sorry, we couldn't find your quiz results. Please try again.</p>
            <Button onClick={() => setSelectedQuiz(null)}>Back to Quizzes</Button>
          </div>
        );
        
        const score = calculateScore(quiz);
        
        return (
          <div className="max-w-4xl mx-auto space-y-6">
            <Card className="text-center">
              <CardContent className="pt-8 pb-8">
                <Trophy className="h-16 w-16 text-yellow-500 mx-auto mb-4" />
                <h2 className="text-3xl font-bold text-gray-900 mb-2">Quiz Complete!</h2>
                <p className="text-xl text-gray-600 mb-4">Your Score: {score}%</p>
                <div className="flex justify-center space-x-4">
                  <Button onClick={() => {
                    setShowResults(false);
                    setCurrentQuestion(0);
                    setCurrentAnswers(quiz.documentId, {});
                  }}>
                    <RotateCcw className="h-4 w-4 mr-2" />
                    Retake Quiz
                  </Button>
                  <Button variant="outline" onClick={() => setSelectedQuiz(null)}>
                    Back to Quizzes
                  </Button>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Answer Review</CardTitle>
              </CardHeader>
              <CardContent className="space-y-6">
                {quiz.questions.map((question, index) => {
                  const userAnswer = getCurrentAnswers(quiz.documentId)[index];
                  let isCorrect = false;
                  
                  if (question.options) {
                    const userOption = userAnswer !== undefined ? question.options[userAnswer] : '';
                    const userOptionNorm = userOption ? userOption.trim().toLowerCase() : '';
                    const correctOptionNorm = question.correctAnswer ? question.correctAnswer.trim().toLowerCase() : '';
                    isCorrect = userOptionNorm === correctOptionNorm;
                  } else {
                    const userAnswerStr = typeof userAnswer === 'string' ? userAnswer : '';
                    const userAnswerTrimmed = userAnswerStr ? userAnswerStr.trim().toLowerCase() : '';
                    const correctAnswerTrimmed = question.correctAnswer ? question.correctAnswer.trim().toLowerCase() : '';
                    isCorrect = !!correctAnswerTrimmed && userAnswerTrimmed === correctAnswerTrimmed;
                  }
                  
                  return (
                    <div key={index} className="border-b border-gray-100 pb-6 last:border-b-0">
                      <div className="flex items-start space-x-3 mb-3">
                        {isCorrect ? (
                          <CheckCircle className="h-6 w-6 text-green-500 mt-1" />
                        ) : (
                          <XCircle className="h-6 w-6 text-red-500 mt-1" />
                        )}
                        <div className="flex-1">
                          <h4 className="font-medium text-gray-900 mb-2">{question.question}</h4>
                          <div className="space-y-2">
                            {question.options ? (
                              question.options.map((option, optionIndex) => {
                                let className = "p-2 rounded text-sm ";
                                const isUserSelected = optionIndex === userAnswer;
                                const isCorrectOption = option === question.correctAnswer;
                                
                                if (isCorrectOption) {
                                  className += "bg-green-100 text-green-800 border border-green-200";
                                } else if (isUserSelected && !isCorrect) {
                                  className += "bg-red-100 text-red-800 border border-red-200";
                                } else {
                                  className += "bg-gray-50 text-gray-700";
                                }
                                
                                return (
                                  <div key={optionIndex} className={className}>
                                    {option}
                                    {isCorrectOption && <span className="ml-2 text-green-600 font-semibold">✓ Correct</span>}
                                    {isUserSelected && !isCorrectOption && <span className="ml-2 text-red-600 font-semibold">✗ Your choice</span>}
                                  </div>
                                );
                              })
                            ) : (
                              <div className="space-y-2">
                                <div className={`p-2 rounded text-sm ${isCorrect ? 'bg-green-100 text-green-800 border border-green-200' : 'bg-red-100 text-red-800 border border-red-200'}`}>
                                  <span className="font-semibold">Your answer:</span> {userAnswer ? userAnswer : <span className="italic text-gray-400">No answer</span>}
                                </div>
                                <div className="p-2 rounded text-sm bg-green-100 text-green-800 border border-green-200">
                                  <span className="font-semibold">Correct answer:</span> {question.correctAnswer}
                                </div>
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </CardContent>
            </Card>
          </div>
        );
      })()}

      {/* Empty state */}
      {uploadedFiles.length === 0 || documentIds.length === 0 ? (
        <Card className="text-center py-12">
          <CardContent>
            <Brain className="h-16 w-16 text-gray-400 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-gray-900 mb-2">No quizzes available</h3>
            <p className="text-gray-600 mb-6">Upload your course materials to generate personalized quizzes</p>
            <Button variant="outline">Go to Upload Tab</Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold text-gray-900">AI-Generated Quizzes</h2>
              <p className="text-gray-600">Test your knowledge with personalized quizzes</p>
            </div>
          </div>

          {readyQuizzes.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-gray-900">Available Quizzes</h3>
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {readyQuizzes.map((quiz, index) => (
                  <Card key={quiz.documentId} className="hover:shadow-lg transition-shadow cursor-pointer" onClick={() => setSelectedQuiz(index)}>
                    <CardHeader>
                      <CardTitle className="text-lg">{quiz.documentName}</CardTitle>
                      <CardDescription>
                        {quiz.questions.length} questions • Multiple choice & short answer
                      </CardDescription>
                    </CardHeader>
                    <CardContent>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Ready to start</span>
                        <Button size="sm">Start Quiz</Button>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {pendingDocuments.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-gray-900">Quizzes Being Generated</h3>
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {pendingDocuments.map((quiz) => (
                  <Card key={quiz.documentId} className="border-yellow-200 bg-yellow-50">
                    <CardContent className="pt-6">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-3">
                          <Loader2 className="h-5 w-5 text-yellow-600 animate-spin" />
                          <div>
                            <h4 className="font-medium text-gray-900">{quiz.documentName}</h4>
                            <p className="text-sm text-gray-600">AI is generating your quiz...</p>
                          </div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {quizzes.filter(q => q.status === 'ERROR').length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-gray-900">Failed Quizzes</h3>
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {quizzes.filter(q => q.status === 'ERROR').map((quiz) => (
                  <Card key={quiz.documentId} className="border-red-200 bg-red-50">
                    <CardContent className="pt-6">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-3">
                          <div>
                            <h4 className="font-medium text-gray-900">{quiz.documentName}</h4>
                            <p className="text-sm text-red-600">{quiz.error || 'Failed to generate quiz'}</p>
                          </div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {quizzes.filter(q => q.status === 'PENDING').length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-gray-900">Pending Quizzes</h3>
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {quizzes.filter(q => q.status === 'PENDING').map((quiz) => (
                  <Card key={quiz.documentId} className="border-gray-200 bg-gray-50">
                    <CardContent className="pt-6">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-3">
                          <FileText className="h-5 w-5 text-gray-600" />
                          <div>
                            <h4 className="font-medium text-gray-900">{quiz.documentName}</h4>
                            <p className="text-sm text-gray-600">Quiz generation hasn't started yet</p>
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

          {readyQuizzes.length === 0 && pendingDocuments.length === 0 && quizzes.filter(q => q.status === 'ERROR').length === 0 && quizzes.filter(q => q.status === 'PENDING').length === 0 && (
            <Card className="text-center py-12">
              <CardContent>
                <Brain className="h-16 w-16 text-gray-400 mx-auto mb-4" />
                <h3 className="text-xl font-semibold text-gray-900 mb-2">No quizzes available</h3>
                <p className="text-gray-600 mb-6">Upload your course materials to generate personalized quizzes</p>
                <Button variant="outline">Go to Upload Tab</Button>
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </>
  );
};

export default QuizTab;