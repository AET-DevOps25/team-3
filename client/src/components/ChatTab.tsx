
import { useState, useRef, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { MessageSquare, Send, Bot, User, Sparkles, FileText, Clock } from 'lucide-react';

interface ChatTabProps {
  uploadedFiles: File[];
}

interface Message {
  id: string;
  content: string;
  sender: 'user' | 'bot';
  timestamp: Date;
  sources?: string[];
}

const ChatTab = ({ uploadedFiles }: ChatTabProps) => {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      content: "Hi! I'm your AI study assistant. I've analyzed your uploaded course materials and I'm ready to help you understand the concepts, clarify doubts, and answer questions. What would you like to know?",
      sender: 'bot',
      timestamp: new Date(),
    }
  ]);
  const [inputMessage, setInputMessage] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const simulateBotResponse = (userMessage: string): string => {
    const lowerMessage = userMessage.toLowerCase();
    
    if (lowerMessage.includes('machine learning') || lowerMessage.includes('ml')) {
      return "Machine Learning is a subset of artificial intelligence that enables computers to learn and make decisions from data without being explicitly programmed. Based on your uploaded materials, I can see you're studying supervised learning (which uses labeled data) and unsupervised learning (which finds patterns in unlabeled data). Would you like me to explain any specific ML algorithm or concept?";
    }
    
    if (lowerMessage.includes('overfitting')) {
      return "Overfitting occurs when a model learns the training data too well, including noise and irrelevant details. This means it performs great on training data but poorly on new, unseen data. It's like memorizing answers to practice questions without understanding the concepts - you'd fail when faced with new questions. To prevent overfitting, we can use techniques like cross-validation, regularization, or reducing model complexity.";
    }
    
    if (lowerMessage.includes('probability') || lowerMessage.includes('distribution')) {
      return "From your Statistics materials, I can help explain probability distributions! The normal distribution is bell-shaped and describes many natural phenomena. The binomial distribution models scenarios with yes/no outcomes (like coin flips), while the Poisson distribution describes rare events over time (like customer arrivals). Which specific distribution would you like me to explain in more detail?";
    }
    
    if (lowerMessage.includes('quiz') || lowerMessage.includes('test')) {
      return "I'd be happy to help you prepare for your quiz! Based on your materials, I can create practice questions on machine learning fundamentals or probability distributions. I can also explain concepts that commonly appear on exams. What specific topic would you like to focus on for your test preparation?";
    }
    
    return "That's a great question! Based on your course materials, I can provide detailed explanations about the concepts you're studying. Could you be more specific about which topic you'd like me to explain? I have information about machine learning algorithms, probability distributions, and statistical concepts from your uploaded files.";
  };

  const handleSendMessage = async () => {
    if (!inputMessage.trim()) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      content: inputMessage,
      sender: 'user',
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsTyping(true);

    // Simulate AI response delay
    setTimeout(() => {
      const botResponse: Message = {
        id: (Date.now() + 1).toString(),
        content: simulateBotResponse(inputMessage),
        sender: 'bot',
        timestamp: new Date(),
        sources: ['Machine Learning Lecture 1.pdf', 'Statistics Chapter 5.pdf']
      };

      setMessages(prev => [...prev, botResponse]);
      setIsTyping(false);
    }, 1500);
  };

  const suggestedQuestions = [
    "Explain the difference between supervised and unsupervised learning",
    "What is overfitting and how can I prevent it?",
    "Help me understand probability distributions",
    "Create a quiz question about machine learning"
  ];

  if (uploadedFiles.length === 0) {
    return (
      <Card className="text-center py-12">
        <CardContent>
          <MessageSquare className="h-16 w-16 text-gray-400 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-gray-900 mb-2">AI Chat not available</h3>
          <p className="text-gray-600 mb-6">Upload your course materials to chat with your AI tutor</p>
          <Button variant="outline">Go to Upload Tab</Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="h-[calc(100vh-12rem)] flex flex-col space-y-4">
      {/* Chat Header */}
      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-500 rounded-full flex items-center justify-center">
              <Bot className="h-6 w-6 text-white" />
            </div>
            <div>
              <CardTitle className="text-lg">AI Study Assistant</CardTitle>
              <CardDescription className="flex items-center space-x-4">
                <span className="flex items-center">
                  <FileText className="h-3 w-3 mr-1" />
                  {uploadedFiles.length} files analyzed
                </span>
                <span className="flex items-center text-green-600">
                  <div className="w-2 h-2 bg-green-500 rounded-full mr-1"></div>
                  Online
                </span>
              </CardDescription>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Messages Area */}
      <Card className="flex-1 flex flex-col">
        <CardContent className="flex-1 p-0 flex flex-col">
          {/* Messages Container */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${message.sender === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div className={`flex space-x-3 max-w-3xl ${message.sender === 'user' ? 'flex-row-reverse space-x-reverse' : ''}`}>
                  {/* Avatar */}
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${
                    message.sender === 'user' 
                      ? 'bg-blue-500' 
                      : 'bg-gradient-to-br from-purple-500 to-blue-500'
                  }`}>
                    {message.sender === 'user' ? (
                      <User className="h-4 w-4 text-white" />
                    ) : (
                      <Bot className="h-4 w-4 text-white" />
                    )}
                  </div>

                  {/* Message Content */}
                  <div className={`flex flex-col space-y-2 ${message.sender === 'user' ? 'items-end' : 'items-start'}`}>
                    <div
                      className={`px-4 py-3 rounded-2xl max-w-full ${
                        message.sender === 'user'
                          ? 'bg-blue-500 text-white'
                          : 'bg-gray-100 text-gray-900'
                      }`}
                    >
                      <p className="text-sm leading-relaxed">{message.content}</p>
                    </div>

                    {/* Sources and Timestamp */}
                    <div className="flex items-center space-x-2 text-xs text-gray-500">
                      <Clock className="h-3 w-3" />
                      <span>{message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                      {message.sources && (
                        <>
                          <span>•</span>
                          <div className="flex items-center space-x-1">
                            <Sparkles className="h-3 w-3" />
                            <span>Sources: {message.sources.slice(0, 2).join(', ')}</span>
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}

            {/* Typing Indicator */}
            {isTyping && (
              <div className="flex justify-start">
                <div className="flex space-x-3 max-w-3xl">
                  <div className="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-blue-500 flex items-center justify-center flex-shrink-0">
                    <Bot className="h-4 w-4 text-white" />
                  </div>
                  <div className="bg-gray-100 rounded-2xl px-4 py-3">
                    <div className="flex space-x-1">
                      <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                      <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-100"></div>
                      <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-200"></div>
                    </div>
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Suggested Questions */}
          {messages.length === 1 && (
            <div className="px-6 pb-4">
              <p className="text-sm text-gray-600 mb-3">Try asking:</p>
              <div className="grid gap-2 md:grid-cols-2">
                {suggestedQuestions.map((question, index) => (
                  <Button
                    key={index}
                    variant="outline"
                    size="sm"
                    className="text-left justify-start h-auto py-3 px-4 text-wrap"
                    onClick={() => setInputMessage(question)}
                  >
                    {question}
                  </Button>
                ))}
              </div>
            </div>
          )}

          {/* Input Area */}
          <div className="border-t border-gray-200 p-6">
            <div className="flex space-x-3">
              <Input
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                placeholder="Ask a question about your course materials..."
                onKeyDown={(e) => e.key === 'Enter' && handleSendMessage()}
                className="flex-1"
              />
              <Button 
                onClick={handleSendMessage}
                disabled={!inputMessage.trim() || isTyping}
                className="bg-blue-500 hover:bg-blue-600"
              >
                <Send className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default ChatTab;
