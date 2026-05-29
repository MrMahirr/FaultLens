const fs = require('fs');
let data = fs.readFileSync('shared/mocks/data.ts', 'utf8');

// Update MockLogGroup interface
data = data.replace(/export interface MockLogGroup \{[\s\S]*?\}/, `export interface MockLogGroup {
  id: number;
  fingerprint: string;
  firstMessage: string;
  count: number;
  severity: Severity;
  firstSeenAt: string;
  lastSeenAt: string;
}`);

// Replace firstSeen and lastSeen, remove source and hasAnalysis
data = data.replace(/firstSeen:\s*(.*?),\n\s*lastSeen:\s*(.*?),\n\s*source:\s*.*?,\n\s*hasAnalysis:\s*.*?,/g, 'firstSeenAt: $1,\n    lastSeenAt: $2,');

fs.writeFileSync('shared/mocks/data.ts', data);
console.log('Mock groups updated');
