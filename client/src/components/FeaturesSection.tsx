
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { FileText, Brain, MessageSquare, BookOpen, Target, Users, Zap, Clock } from 'lucide-react';

const FeaturesSection = () => {
  const features = [
    {
      icon: FileText,
      title: "Smart Summaries",
      description: "AI generates clear, concise summaries of your lecture slides and transcripts automatically.",
      color: "bg-blue-500"
    },
    {
      icon: Brain,
      title: "Quiz Generation",
      description: "Create practice tests and quizzes tailored to your course material for better retention.",
      color: "bg-purple-500"
    },
    {
      icon: BookOpen,
      title: "Interactive Flashcards",
      description: "Key concepts transformed into flashcards for efficient revision and memorization.",
      color: "bg-green-500"
    },
    {
      icon: MessageSquare,
      title: "AI Chatbot Tutor",
      description: "Ask questions and get instant clarifications about your course material 24/7.",
      color: "bg-orange-500"
    },
    {
      icon: Target,
      title: "Adaptive Learning",
      description: "AI learns from your performance and recommends personalized study paths.",
      color: "bg-red-500"
    },
    {
      icon: Clock,
      title: "Quick Catch-up",
      description: "Missed a lecture? Upload slides and get up to speed in minutes, not hours.",
      color: "bg-indigo-500"
    }
  ];

  const stats = [
    { label: "Students Helped", value: "10K+", icon: Users },
    { label: "Course Materials Processed", value: "50K+", icon: FileText },
    { label: "Study Hours Saved", value: "100K+", icon: Clock },
    { label: "Quiz Questions Generated", value: "1M+", icon: Brain }
  ];

  return (
    <div className="py-24 bg-white">
      <div className="mx-auto max-w-7xl px-6 lg:px-8">
        {/* Features Grid */}
        <div className="mx-auto max-w-4xl text-center mb-16">
          <h2 className="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl mb-4">
            Everything you need to excel in your studies
          </h2>
          <p className="text-xl text-gray-600">
            Powerful AI tools designed specifically for university students
          </p>
        </div>

        <div className="grid gap-8 lg:grid-cols-3 md:grid-cols-2 grid-cols-1 mb-20">
          {features.map((feature, index) => {
            const Icon = feature.icon;
            return (
              <Card key={index} className="group hover:shadow-xl transition-all duration-300 transform hover:-translate-y-2 border-0 shadow-lg">
                <CardHeader className="text-center pb-4">
                  <div className={`w-12 h-12 ${feature.color} rounded-full flex items-center justify-center mx-auto mb-4 group-hover:scale-110 transition-transform duration-300`}>
                    <Icon className="h-6 w-6 text-white" />
                  </div>
                  <CardTitle className="text-xl font-semibold text-gray-900">
                    {feature.title}
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <CardDescription className="text-gray-600 text-center leading-relaxed">
                    {feature.description}
                  </CardDescription>
                </CardContent>
              </Card>
            );
          })}
        </div>

        {/* Stats Section */}
        <div className="bg-gradient-to-r from-blue-600 to-purple-600 rounded-3xl p-12 text-white">
          <div className="text-center mb-12">
            <h3 className="text-3xl font-bold mb-4">Trusted by students worldwide</h3>
            <p className="text-blue-100 text-lg">Join thousands of students who are already studying smarter</p>
          </div>
          <div className="grid gap-8 lg:grid-cols-4 md:grid-cols-2 grid-cols-1">
            {stats.map((stat, index) => {
              const Icon = stat.icon;
              return (
                <div key={index} className="text-center">
                  <Icon className="h-8 w-8 text-white mx-auto mb-3" />
                  <div className="text-3xl font-bold mb-2">{stat.value}</div>
                  <div className="text-blue-100">{stat.label}</div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};

export default FeaturesSection;
