
import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { BookOpen, RotateCcw, ChevronLeft, ChevronRight, Shuffle, Star, Eye, EyeOff } from 'lucide-react';

interface FlashcardsTabProps {
  uploadedFiles: File[];
}

const FlashcardsTab = ({ uploadedFiles }: FlashcardsTabProps) => {
  const [selectedDeck, setSelectedDeck] = useState<number | null>(null);
  const [currentCard, setCurrentCard] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [studyMode, setStudyMode] = useState(false);

  const mockDecks = [
    {
      id: 1,
      title: "Machine Learning Key Terms",
      description: "Essential concepts and definitions",
      cardCount: 12,
      source: "Machine Learning Lecture 1.pdf",
      difficulty: "Beginner"
    },
    {
      id: 2,
      title: "Probability Distributions",
      description: "Formulas and properties of common distributions",
      cardCount: 8,
      source: "Statistics Chapter 5.pdf",
      difficulty: "Intermediate"
    }
  ];

  const mockCards = [
    {
      front: "What is Supervised Learning?",
      back: "A type of machine learning where algorithms learn from labeled training data to make predictions or decisions. The algorithm is provided with input-output pairs and learns to map inputs to correct outputs."
    },
    {
      front: "Define Overfitting",
      back: "A modeling error that occurs when a machine learning model learns the training data too well, including noise and random fluctuations. This results in poor performance on new, unseen data."
    },
    {
      front: "What is Cross-Validation?",
      back: "A resampling technique used to evaluate machine learning models by dividing the dataset into multiple subsets, training on some subsets and testing on others to assess model performance."
    },
    {
      front: "Explain Bias-Variance Tradeoff",
      back: "The balance between a model's ability to minimize bias (error from oversimplifying assumptions) and variance (error from sensitivity to small fluctuations in training data). Lower bias often means higher variance and vice versa."
    }
  ];

  const nextCard = () => {
    setCurrentCard((prev) => (prev + 1) % mockCards.length);
    setIsFlipped(false);
  };

  const prevCard = () => {
    setCurrentCard((prev) => (prev - 1 + mockCards.length) % mockCards.length);
    setIsFlipped(false);
  };

  const shuffleDeck = () => {
    setCurrentCard(Math.floor(Math.random() * mockCards.length));
    setIsFlipped(false);
  };

  if (uploadedFiles.length === 0) {
    return (
      <Card className="text-center py-12">
        <CardContent>
          <BookOpen className="h-16 w-16 text-gray-400 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-gray-900 mb-2">No flashcards available</h3>
          <p className="text-gray-600 mb-6">Upload your course materials to generate flashcards</p>
          <Button variant="outline">Go to Upload Tab</Button>
        </CardContent>
      </Card>
    );
  }

  if (selectedDeck !== null) {
    return (
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Study Header */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-xl">Machine Learning Key Terms</CardTitle>
                <CardDescription>
                  Card {currentCard + 1} of {mockCards.length}
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
                style={{ width: `${((currentCard + 1) / mockCards.length) * 100}%` }}
              ></div>
            </div>
          </CardHeader>
        </Card>

        {/* Flashcard */}
        <div className="flex justify-center">
          <div 
            className="relative w-full max-w-2xl h-80 cursor-pointer"
            onClick={() => setIsFlipped(!isFlipped)}
          >
            <div className={`absolute inset-0 w-full h-full transition-transform duration-600 transform-style-preserve-3d ${isFlipped ? 'rotate-y-180' : ''}`}>
              {/* Front */}
              <Card className="absolute inset-0 w-full h-full backface-hidden border-2 border-purple-200 shadow-lg hover:shadow-xl transition-shadow">
                <CardContent className="h-full flex flex-col items-center justify-center p-8 text-center">
                  <div className="mb-4">
                    <Eye className="h-8 w-8 text-purple-500 mx-auto" />
                  </div>
                  <h3 className="text-2xl font-bold text-gray-900 mb-4">
                    {mockCards[currentCard].front}
                  </h3>
                  <p className="text-gray-600">Click to reveal answer</p>
                </CardContent>
              </Card>

              {/* Back */}
              <Card className="absolute inset-0 w-full h-full backface-hidden rotate-y-180 border-2 border-green-200 shadow-lg bg-green-50">
                <CardContent className="h-full flex flex-col items-center justify-center p-8 text-center">
                  <div className="mb-4">
                    <EyeOff className="h-8 w-8 text-green-500 mx-auto" />
                  </div>
                  <p className="text-lg text-gray-800 leading-relaxed">
                    {mockCards[currentCard].back}
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>

        {/* Navigation */}
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

        {/* Study Actions */}
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
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Flashcard Decks</h2>
          <p className="text-gray-600">Master key concepts with spaced repetition</p>
        </div>
        <Button className="bg-purple-600 hover:bg-purple-700">
          Create New Deck
        </Button>
      </div>

      {/* Deck Cards */}
      <div className="grid gap-6 md:grid-cols-2">
        {mockDecks.map((deck, index) => (
          <Card key={deck.id} className="hover:shadow-lg transition-shadow cursor-pointer group">
            <CardHeader>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <CardTitle className="text-lg text-gray-900 group-hover:text-purple-600 transition-colors">
                    {deck.title}
                  </CardTitle>
                  <CardDescription className="mt-2">{deck.description}</CardDescription>
                </div>
                <Star className="h-5 w-5 text-gray-400 hover:text-yellow-500 cursor-pointer" />
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between text-sm text-gray-600">
                <span className="flex items-center">
                  <BookOpen className="h-4 w-4 mr-1" />
                  {deck.cardCount} cards
                </span>
                <span className="text-purple-600 font-medium">
                  {deck.difficulty}
                </span>
              </div>
              
              <div className="text-xs text-gray-500">
                Source: {deck.source}
              </div>
              
              <div className="flex space-x-2">
                <Button 
                  className="flex-1"
                  onClick={() => setSelectedDeck(deck.id)}
                >
                  Study Now
                </Button>
                <Button variant="outline" size="sm">
                  Preview
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Study Stats */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Study Progress</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-3 gap-6 text-center">
            <div>
              <div className="text-2xl font-bold text-purple-600">24</div>
              <div className="text-sm text-gray-600">Cards Studied Today</div>
            </div>
            <div>
              <div className="text-2xl font-bold text-green-600">89%</div>
              <div className="text-sm text-gray-600">Average Accuracy</div>
            </div>
            <div>
              <div className="text-2xl font-bold text-blue-600">7</div>
              <div className="text-sm text-gray-600">Day Streak</div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default FlashcardsTab;
