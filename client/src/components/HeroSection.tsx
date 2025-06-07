
import { Button } from '@/components/ui/button';
import { Brain, Zap, Upload } from 'lucide-react';

interface HeroSectionProps {
  onGetStarted: () => void;
}

const HeroSection = ({ onGetStarted }: HeroSectionProps) => {
  return (
    <div className="relative overflow-hidden bg-gradient-to-br from-blue-600 via-purple-600 to-blue-800">
      <div className="absolute inset-0 bg-black/20"></div>
      <div className="relative px-6 py-24 sm:px-12 sm:py-32 lg:px-16">
        <div className="mx-auto max-w-4xl text-center">
          <div className="flex justify-center mb-8">
            <div className="flex items-center space-x-3 bg-white/10 backdrop-blur-sm rounded-full px-6 py-3 border border-white/20">
              <Brain className="h-8 w-8 text-white" />
              <span className="text-2xl font-bold text-white">StudyMate</span>
            </div>
          </div>
          
          <h1 className="text-4xl sm:text-6xl font-bold text-white mb-6 leading-tight">
            Transform Your
            <span className="bg-gradient-to-r from-yellow-300 to-orange-300 bg-clip-text text-transparent"> Lectures </span>
            Into Interactive Learning
          </h1>
          
          <p className="text-xl text-blue-100 mb-10 max-w-3xl mx-auto leading-relaxed">
            Turn passive lecture slides and transcripts into an interactive study companion. 
            Get AI-powered summaries, quizzes, flashcards, and a chatbot tutor that knows your course material.
          </p>
          
          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
            <Button 
              onClick={onGetStarted}
              size="lg" 
              className="bg-white text-blue-600 hover:bg-blue-50 px-8 py-4 text-lg font-semibold rounded-full shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105"
            >
              <Upload className="mr-2 h-5 w-5" />
              Get Started Free
            </Button>
            <Button 
              variant="outline" 
              size="lg"
              className="border-white/30 text-white hover:bg-white/10 px-8 py-4 text-lg rounded-full backdrop-blur-sm"
            >
              Watch Demo
            </Button>
          </div>
          
          <div className="mt-16 flex justify-center items-center space-x-8 text-blue-200">
            <div className="flex items-center space-x-2">
              <Zap className="h-5 w-5" />
              <span>AI-Powered</span>
            </div>
            <div className="flex items-center space-x-2">
              <Brain className="h-5 w-5" />
              <span>Interactive Learning</span>
            </div>
            <div className="flex items-center space-x-2">
              <Upload className="h-5 w-5" />
              <span>Easy Upload</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HeroSection;
