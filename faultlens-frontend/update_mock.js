const fs = require('fs');
let data = fs.readFileSync('shared/mocks/data.ts', 'utf8');

data = data.replace(/export interface MockLogEntry \{[\s\S]*?\}/, `export interface MockLogEntry {
  id: number;
  sourceId: number;
  groupId?: number;
  severity: Severity;
  message: string;
  parsedMessage?: string;
  stackTrace?: string;
  namespace?: string;
  podName?: string;
  containerName?: string;
  serviceName?: string;
  cluster?: string;
  timestamp: string;
}`);

data = data.replace(/source:\s*['"][^'"]+['"],\s*sourceType:\s*LogSourceType\.[A-Z]+,/g, 'sourceId: 1,');

fs.writeFileSync('shared/mocks/data.ts', data);
console.log('Mock veriler güncellendi');
