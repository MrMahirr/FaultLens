const fs = require('fs');
let data = fs.readFileSync('shared/mocks/data.ts', 'utf8');

// Update MockSource interface to LogSourceDto
data = data.replace(/export interface MockSource \{[\s\S]*?\}/, `export interface LogSourceDto {
  id: number;
  name: string;
  type: LogSourceType;
  config: string;
  status?: ConnectionStatus;
  enabled: boolean;
  lastSeenAt: string;
  createdAt: string;
}`);

// Replace MockSource type references
data = data.replace(/MockSource/g, 'LogSourceDto');
data = data.replace(/mockSources:\s*LogSourceDto\[\]/g, 'mockSources: LogSourceDto[]');

// Fix the array entries inside mockSources
// Regex to capture each object in mockSources
data = data.replace(/namespace:\s*(".*?"),\s*status:\s*(ConnectionStatus\.[A-Z]+),\s*enabled:\s*(true|false),\s*lastLogAt:\s*(.*?),\s*logCount:\s*\d+,\s*createdAt:\s*(".*?")/g, 
  (match, namespace, status, enabled, lastLogAt, createdAt) => {
    return `config: JSON.stringify({ namespace: ${namespace} }),
    status: ${status},
    enabled: ${enabled},
    lastSeenAt: ${lastLogAt},
    createdAt: ${createdAt}`;
});

data = data.replace(/host:\s*(".*?"),\s*status:\s*(ConnectionStatus\.[A-Z]+),\s*enabled:\s*(true|false),\s*lastLogAt:\s*(.*?),\s*logCount:\s*\d+,\s*createdAt:\s*(".*?")/g, 
  (match, host, status, enabled, lastLogAt, createdAt) => {
    return `config: JSON.stringify({ host: ${host} }),
    status: ${status},
    enabled: ${enabled},
    lastSeenAt: ${lastLogAt},
    createdAt: ${createdAt}`;
});

fs.writeFileSync('shared/mocks/data.ts', data);
console.log('Mock sources updated');
