const fs = require('fs');
let data = fs.readFileSync('shared/mocks/data.ts', 'utf8');

// Update MockAnalysis interface to AnalysisResultDto
data = data.replace(/export interface MockAnalysis \{[\s\S]*?\}/, `export interface AnalysisResultDto {
  id: number;
  logGroupId?: number;
  logEntryId?: number;
  rootCause: string;
  suggestion: string;
  affectedDeployment?: string;
  confidenceScore: number;
  engineType: AnalysisType;
  analyzedAt: string;
}`);

// Replace MockAnalysis type references
data = data.replace(/MockAnalysis/g, 'AnalysisResultDto');
data = data.replace(/mockAnalyses:\s*AnalysisResultDto\[\]/g, 'mockAnalyses: AnalysisResultDto[]');

// Fix the array entries inside mockAnalyses
// Regex to capture each object in mockAnalyses
data = data.replace(/groupId:\s*(\d+),\n\s*groupMessage:\s*".*?",\n\s*type:\s*(AnalysisType\.[A-Z_]+),\n\s*rootCause:\s*(".*?"|`[\s\S]*?`),\n\s*suggestion:\s*(".*?"|`[\s\S]*?`),\n\s*confidence:\s*([\d.]+),\n\s*(?:deploymentCorrelation:\s*\{[\s\S]*?\},)?\n\s*createdAt:\s*(.*?)(?=\n\s*\})/g, 
  (match, groupId, type, rootCause, suggestion, confidence, createdAt) => {
    return `logGroupId: ${groupId},
    engineType: ${type},
    rootCause: ${rootCause},
    suggestion: ${suggestion},
    confidenceScore: ${confidence},
    affectedDeployment: "payment-service:v2.3.1",
    analyzedAt: ${createdAt}`;
});

fs.writeFileSync('shared/mocks/data.ts', data);
console.log('Mock analyses updated');
