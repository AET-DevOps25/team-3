
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { FileText, Clock, BookOpen, Download, Sparkles } from 'lucide-react';

interface SummaryTabProps {
  uploadedFiles: File[];
}

const SummaryTab = ({ uploadedFiles }: SummaryTabProps) => {
  const mockSummaries = [
    {
      file: "Machine Learning Lecture 1.pdf",
      title: "Introduction to Machine Learning",
      summary: "This lecture covers the fundamental concepts of machine learning, including supervised and unsupervised learning. Key topics include linear regression, classification algorithms, and the importance of training vs. test data. The lecture emphasizes practical applications in real-world scenarios and common pitfalls to avoid when building ML models.",
      keyPoints: [
        "Machine learning is a subset of artificial intelligence",
        "Supervised learning uses labeled training data",
        "Unsupervised learning finds patterns in unlabeled data",
        "Overfitting occurs when models memorize training data"
      ],
      readTime: "3 min read"
    },
    {
      file: "Statistics Chapter 5.pdf",
      title: "Probability Distributions",
      summary: "Chapter 5 explores various probability distributions including normal, binomial, and Poisson distributions. The material covers how to calculate probabilities, understand distribution parameters, and apply these concepts to real-world statistical problems.",
      keyPoints: [
        "Normal distribution is bell-shaped and symmetric",
        "Binomial distribution models discrete yes/no outcomes",
        "Poisson distribution describes rare events over time",
        "Central Limit Theorem explains why normal distribution is common"
      ],
      readTime: "4 min read"
    }
  ];

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

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">AI-Generated Summaries</h2>
          <p className="text-gray-600">Clear, concise summaries of your course materials</p>
        </div>
        <Button className="bg-purple-600 hover:bg-purple-700">
          <Download className="h-4 w-4 mr-2" />
          Export All
        </Button>
      </div>

      {/* Summaries Grid */}
      <div className="grid gap-6">
        {mockSummaries.map((item, index) => (
          <Card key={index} className="hover:shadow-lg transition-shadow">
            <CardHeader>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <CardTitle className="text-xl text-gray-900 mb-2">{item.title}</CardTitle>
                  <CardDescription className="flex items-center space-x-4 text-sm">
                    <span className="flex items-center">
                      <FileText className="h-4 w-4 mr-1" />
                      {item.file}
                    </span>
                    <span className="flex items-center">
                      <Clock className="h-4 w-4 mr-1" />
                      {item.readTime}
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
                <p className="text-gray-700 leading-relaxed">{item.summary}</p>
              </div>

              {/* Key Points */}
              <div>
                <h4 className="font-semibold text-gray-900 mb-3 flex items-center">
                  <BookOpen className="h-4 w-4 mr-2" />
                  Key Points
                </h4>
                <ul className="space-y-2">
                  {item.keyPoints.map((point, pointIndex) => (
                    <li key={pointIndex} className="flex items-start">
                      <div className="w-2 h-2 bg-purple-500 rounded-full mt-2 mr-3 flex-shrink-0"></div>
                      <span className="text-gray-700">{point}</span>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Actions */}
              <div className="flex space-x-3 pt-4 border-t border-gray-100">
                <Button variant="outline" size="sm">
                  <Download className="h-4 w-4 mr-2" />
                  Export PDF
                </Button>
                <Button variant="outline" size="sm">
                  Generate Quiz
                </Button>
                <Button variant="outline" size="sm">
                  Create Flashcards
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
};

export default SummaryTab;
