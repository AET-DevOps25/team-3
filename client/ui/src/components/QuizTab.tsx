
import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Brain, Clock, CheckCircle, XCircle, Trophy, RotateCcw } from 'lucide-react';

interface QuizTabProps {
  uploadedFiles: File[];
}

const QuizTab = ({ uploadedFiles }: QuizTabProps) => {
  const [selectedQuiz, setSelectedQuiz] = useState<number | null>(null);
  const [currentQuestion, setCurrentQuestion] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState<{ [key: number]: number }>({});
  const [showResults, setShowResults] = useState(false);

  const mockQuizzes = [
    {
      id: 1,
      title: "Machine Learning Fundamentals",
      description: "Test your understanding of basic ML concepts",
      questions: 8,
      difficulty: "Intermediate",
      estimatedTime: "10 min",
      source: "Machine Learning Lecture 1.pdf"
    },
    {
      id: 2,
      title: "Probability Distributions Quiz",
      description: "Quiz on normal, binomial, and Poisson distributions",
      questions: 6,
      difficulty: "Advanced",
      estimatedTime: "8 min",
      source: "Statistics Chapter 5.pdf"
    }
  ];

  const mockQuestions = [
    {
      question: "What is the main difference between supervised and unsupervised learning?",
      options: [
        "Supervised learning uses labeled data, unsupervised doesn't",
        "Supervised learning is faster than unsupervised learning",
        "Supervised learning only works with numerical data",
        "There is no difference between them"
      ],
      correct: 0
    },
    {
      question: "Which of the following is an example of overfitting?",
      options: [
        "Model performs well on both training and test data",
        "Model performs poorly on both training and test data",
        "Model performs well on training data but poorly on test data",
        "Model performs poorly on training data but well on test data"
      ],
      correct: 2
    }
  ];

  const handleAnswerSelect = (questionIndex: number, answerIndex: number) => {
    setSelectedAnswers(prev => ({
      ...prev,
      [questionIndex]: answerIndex
    }));
  };

  const handleSubmitQuiz = () => {
    setShowResults(true);
  };

  const calculateScore = () => {
    let correct = 0;
    mockQuestions.forEach((question, index) => {
      if (selectedAnswers[index] === question.correct) {
        correct++;
      }
    });
    return Math.round((correct / mockQuestions.length) * 100);
  };

  if (uploadedFiles.length === 0) {
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

  if (selectedQuiz !== null && !showResults) {
    return (
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Quiz Header */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-xl">Machine Learning Fundamentals</CardTitle>
                <CardDescription>Question {currentQuestion + 1} of {mockQuestions.length}</CardDescription>
              </div>
              <Button variant="outline" onClick={() => setSelectedQuiz(null)}>
                Exit Quiz
              </Button>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div 
                className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                style={{ width: `${((currentQuestion + 1) / mockQuestions.length) * 100}%` }}
              ></div>
            </div>
          </CardHeader>
        </Card>

        {/* Question Card */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg leading-relaxed">
              {mockQuestions[currentQuestion].question}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {mockQuestions[currentQuestion].options.map((option, index) => (
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
              
              {currentQuestion === mockQuestions.length - 1 ? (
                <Button 
                  onClick={handleSubmitQuiz}
                  disabled={selectedAnswers[currentQuestion] === undefined}
                  className="bg-green-600 hover:bg-green-700"
                >
                  Submit Quiz
                </Button>
              ) : (
                <Button 
                  onClick={() => setCurrentQuestion(prev => Math.min(mockQuestions.length - 1, prev + 1))}
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

  if (showResults) {
    const score = calculateScore();
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
            {mockQuestions.map((question, index) => {
              const userAnswer = selectedAnswers[index];
              const isCorrect = userAnswer === question.correct;
              
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
                        {question.options.map((option, optionIndex) => {
                          let className = "p-2 rounded text-sm ";
                          if (optionIndex === question.correct) {
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
        <Button className="bg-green-600 hover:bg-green-700">
          Generate New Quiz
        </Button>
      </div>

      {/* Quiz Cards */}
      <div className="grid gap-6 md:grid-cols-2">
        {mockQuizzes.map((quiz, index) => (
          <Card key={quiz.id} className="hover:shadow-lg transition-shadow cursor-pointer group">
            <CardHeader>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <CardTitle className="text-lg text-gray-900 group-hover:text-blue-600 transition-colors">
                    {quiz.title}
                  </CardTitle>
                  <CardDescription className="mt-2">{quiz.description}</CardDescription>
                </div>
                <Badge variant={quiz.difficulty === 'Advanced' ? 'destructive' : 'secondary'}>
                  {quiz.difficulty}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between text-sm text-gray-600">
                <span className="flex items-center">
                  <Brain className="h-4 w-4 mr-1" />
                  {quiz.questions} questions
                </span>
                <span className="flex items-center">
                  <Clock className="h-4 w-4 mr-1" />
                  {quiz.estimatedTime}
                </span>
              </div>
              
              <div className="text-xs text-gray-500">
                Source: {quiz.source}
              </div>
              
              <Button 
                className="w-full"
                onClick={() => setSelectedQuiz(quiz.id)}
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
