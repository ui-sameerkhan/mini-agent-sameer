export interface Memory {
  [key: string]: string;
}

const CALC_PATTERN = /(-?\d+(?:\.\d+)?)\s*([+\-*/])\s*(-?\d+(?:\.\d+)?)/;
const REMEMBER_PATTERN = /remember (?:that )?my (.+?) is (.+)/i;
const RECALL_PATTERN = /what(?:'s| is) my (.+?)\??$/i;

function evaluateCalculation(a: number, op: string, b: number): number {
  switch (op) {
    case "+":
      return a + b;
    case "-":
      return a - b;
    case "*":
      return a * b;
    case "/":
      return b === 0 ? NaN : a / b;
    default:
      return NaN;
  }
}

function respond(prompt: string, memory: Memory): string {
  const trimmed = prompt.trim();
  const lower = trimmed.toLowerCase();

  const rememberMatch = trimmed.match(REMEMBER_PATTERN);
  if (rememberMatch) {
    const [, key, value] = rememberMatch;
    memory[key.trim().toLowerCase()] = value.trim();
    return `Got it — I'll remember that your ${key.trim()} is ${value.trim()}.`;
  }

  const recallMatch = trimmed.match(RECALL_PATTERN);
  if (recallMatch) {
    const key = recallMatch[1].trim().toLowerCase();
    if (memory[key]) {
      return `Your ${key} is ${memory[key]}.`;
    }
    return `I don't remember your ${key} yet. Tell me with "Remember my ${key} is ...".`;
  }

  const calcMatch = trimmed.match(CALC_PATTERN);
  if (calcMatch) {
    const [, aStr, op, bStr] = calcMatch;
    const result = evaluateCalculation(parseFloat(aStr), op, parseFloat(bStr));
    if (Number.isNaN(result)) {
      return "I can't compute that — division by zero?";
    }
    return `${aStr} ${op} ${bStr} = ${result}`;
  }

  if (lower.includes("hello") || lower.includes("hi")) {
    return "Hello! I'm your mini agent. Ask me to calculate something, or tell me to remember something for you.";
  }

  if (lower.includes("help")) {
    return [
      "Here's what I can do:",
      '- Calculate: "What is 10 + 5?"',
      '- Remember things: "Remember my cat\'s name is Fluffy"',
      '- Recall things: "What is my cat\'s name?"',
    ].join("\n");
  }

  if (lower.includes("thank")) {
    return "You're welcome! Anything else I can help with?";
  }

  return "I'm a simple mock agent — try asking me a math question, or say \"help\" to see what I can do.";
}

export function mockAgentResponse(
  prompt: string,
  memory: Memory,
): Promise<string> {
  const delay = 600 + Math.random() * 600;
  return new Promise((resolve) => {
    setTimeout(() => resolve(respond(prompt, memory)), delay);
  });
}
