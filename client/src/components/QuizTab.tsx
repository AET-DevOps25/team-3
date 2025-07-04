import { useEffect, useState, useRef } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Brain, Clock, CheckCircle, XCircle, Trophy, RotateCcw, Loader2 } from 'lucide-react';
import { apiService } from '../lib/api';

interface QuizTabProps {
  uploadedFiles: File[];
  documentIds: string[];
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
  error?: string;
}

const QuizTab = ({ uploadedFiles, documentIds }: QuizTabProps) => {
  const [quizzes, setQuizzes] = useState<QuizData[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedQuiz, setSelectedQuiz] = useState<number | null>(null);
  const [currentQuestion, setCurrentQuestion] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState<{ [key: number]: number }>({});
  const [showResults, setShowResults] = useState(false);

  useEffect(() => {
    console.log('QuizTab useEffect triggered, documentIds:', documentIds);
    const fetchQuizzes = async () => {
      // Check if we have already fetched quizzes for these document IDs
      const newDocumentIds = documentIds.filter(id => !fetchedDocumentIds.current.has(id));
      if (newDocumentIds.length === 0) {
        console.log('No new document IDs to fetch quizzes for');
        return;
      }
      console.log('Fetching quizzes for new document IDs:', newDocumentIds);
      if (!newDocumentIds.length) {
        setQuizzes([]);
        return;
      }
      setLoading(true);
      try {
        const quizPromises = newDocumentIds.map(async (documentId) => {
          try {
            const res = await apiService.getQuizForDocument(documentId);
            return res;
          } catch (error) {
            return {
              questions: [],
              documentName: 'Unknown Document',
              documentId,
              error: error instanceof Error ? error.message : 'Failed to fetch quiz',
            };
          }
        });
        const results = await Promise.all(quizPromises);
        // Mark these document IDs as fetched
        newDocumentIds.forEach(id => fetchedDocumentIds.current.add(id));
        setQuizzes(prevQuizzes => {
          // Merge new results with existing quizzes
          const existingQuizzes = prevQuizzes.filter(q => !newDocumentIds.includes(q.documentId));
          return [...existingQuizzes, ...results];
        });
      } finally {
        setLoading(false);
      }
    };
    fetchQuizzes();
  }, [documentIds]);

  const handleAnswerSelect = (questionIndex: number, answerIndex: number) => {
    setSelectedAnswers(prev => ({
      ...prev,
      [questionIndex]: answerIndex
    }));
  };

  const handleSubmitQuiz = () => {
    setShowResults(true);
  };

  const calculateScore = (quiz: QuizData) => {
    let correct = 0;
    quiz.questions.forEach((question, index) => {
      if (
        question.options &&
        selectedAnswers[index] !== undefined &&
        question.options[selectedAnswers[index]] === question.correctAnswer
      ) {
        correct++;
      }
    });
    return quiz.questions.length > 0 ? Math.round((correct / quiz.questions.length) * 100) : 0;
  };

  if (uploadedFiles.length === 0 || documentIds.length === 0) {
    return (
      <Card className="text-center py-12">
        <CardContent>
          <Brain className="h-16 w-16 text-gray-400 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-gray-900 mb-2">No quizzes available</h3>
          <p className="text-gray-600 mb-6">Upload your course materials to generate personalized quizzes</p>
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
          <h3 className="text-xl font-semibold text-gray-900 mb-2">Loading quizzes...</h3>
          <p className="text-gray-600">AI is generating quizzes for your documents</p>
        </div>
      </div>
    );
  }

  if (selectedQuiz !== null && !showResults) {
    const quiz = quizzes[selectedQuiz];
    if (!quiz) return null;
    const question = quiz.questions[currentQuestion];
    return (
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Quiz Header */}
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

        {/* Question Card */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg leading-relaxed">
              {question.question}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {question.options && question.options.map((option, index) => (
              <button
                key={index}
                onClick={() => handleAnswerSelect(currentQuestion, index)}
                className={`w-full p-4 text-left rounded-lg border-2 transition-all duration-200 ${
                  selectedAnswers[currentQuestion] === index
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-blue-300 hover:bg-blue-50/50'
                }`}
              >
                <div className="flex items-center space-x-3">
                  <div className={`w-6 h-6 rounded-full border-2 flex items-center justify-center ${
                    selectedAnswers[currentQuestion] === index
                      ? 'border-blue-500 bg-blue-500'
                      : 'border-gray-300'
                  }`}>
                    {selectedAnswers[currentQuestion] === index && (
                      <div className="w-3 h-3 bg-white rounded-full"></div>
                    )}
                  </div>
                  <span className="text-gray-900">{option}</span>
                </div>
              </button>
            ))}

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
                  disabled={selectedAnswers[currentQuestion] === undefined}
                  className="bg-green-600 hover:bg-green-700"
                >
                  Submit Quiz
                </Button>
              ) : (
                <Button 
                  onClick={() => setCurrentQuestion(prev => Math.min(quiz.questions.length - 1, prev + 1))}
                  disabled={selectedAnswers[currentQuestion] === undefined}
                >
                  Next
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (showResults && selectedQuiz !== null) {
    const quiz = quizzes[selectedQuiz];
    if (!quiz) return null;
    const score = calculateScore(quiz);
    return (
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Results Header */}
        <Card className="text-center">
          <CardContent className="pt-8 pb-8">
            <Trophy className="h-16 w-16 text-yellow-500 mx-auto mb-4" />
            <h2 className="text-3xl font-bold text-gray-900 mb-2">Quiz Complete!</h2>
            <p className="text-xl text-gray-600 mb-4">Your Score: {score}%</p>
            <div className="flex justify-center space-x-4">
              <Button onClick={() => {
                setShowResults(false);
                setCurrentQuestion(0);
                setSelectedAnswers({});
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

        {/* Answer Review */}
        <Card>
          <CardHeader>
            <CardTitle>Answer Review</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            {quiz.questions.map((question, index) => {
              const userAnswer = selectedAnswers[index];
              const isCorrect = question.options && userAnswer !== undefined && question.options[userAnswer] === question.correctAnswer;
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
                        {question.options && question.options.map((option, optionIndex) => {
                          let className = "p-2 rounded text-sm ";
                          if (option === question.correctAnswer) {
                            className += "bg-green-100 text-green-800 border border-green-200";
                          } else if (optionIndex === userAnswer && !isCorrect) {
                            className += "bg-red-100 text-red-800 border border-red-200";
                          } else {
                            className += "bg-gray-50 text-gray-700";
                          }
                          return (
                            <div key={optionIndex} className={className}>
                              {option}
                            </div>
                          );
                        })}
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
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">AI-Generated Quizzes</h2>
          <p className="text-gray-600">Test your knowledge with personalized quizzes</p>
        </div>
      </div>

      {/* Quiz Cards */}
      <div className="grid gap-6 md:grid-cols-2">
        {quizzes.map((quiz, index) => (
          <Card key={quiz.documentId} className="hover:shadow-lg transition-shadow cursor-pointer group">
            <CardHeader>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <CardTitle className="text-lg text-gray-900 group-hover:text-blue-600 transition-colors">
                    {quiz.documentName}
                  </CardTitle>
                  <CardDescription className="mt-2">
                    {quiz.questions.length > 0 ? `${quiz.questions.length} questions` : quiz.error || 'No quiz available'}
                  </CardDescription>
                </div>
                <Badge variant={quiz.questions.length > 7 ? 'destructive' : 'secondary'}>
                  {quiz.questions.length > 7 ? 'Advanced' : 'Standard'}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between text-sm text-gray-600">
                <span className="flex items-center">
                  <Brain className="h-4 w-4 mr-1" />
                  {quiz.questions.length} questions
                </span>
                <span className="flex items-center">
                  <Clock className="h-4 w-4 mr-1" />
                  {Math.max(1, Math.ceil(quiz.questions.length * 1.5))} min
                </span>
              </div>
              <div className="text-xs text-gray-500">
                Source: {quiz.documentName}
              </div>
              <Button 
                className="w-full"
                onClick={() => {
                  setSelectedQuiz(index);
                  setCurrentQuestion(0);
                  setSelectedAnswers({});
                  setShowResults(false);
                }}
                disabled={quiz.questions.length === 0}
              >
                Start Quiz
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
};

export default QuizTab;
