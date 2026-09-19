const express = require('express');
const router = express.Router();
const { GoogleGenerativeAI } = require('@google/generative-ai');

const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY || '');

// ─── Helper: format messages for the AI prompt ────────────────────────────────
function formatMessages(messages) {
  return messages
    .map(m => `[${new Date(m.timestamp).toLocaleTimeString()}] ${m.senderName}: ${m.content}`)
    .join('\n');
}

// ─── POST /api/summarize ─────────────────────────────────────────────────────
// Body: { messages: Message[] }
// Response: SSE stream of text chunks
router.post('/summarize', async (req, res) => {
  const { messages } = req.body;
  if (!messages || messages.length === 0) {
    return res.status(400).json({ error: 'No messages provided' });
  }

  const chat = formatMessages(messages);
  const prompt = `You are a helpful assistant. Analyze this chat conversation and provide a concise summary.
Focus on:
- Key decisions made
- Action items or commitments
- Important information shared
- Open questions or unresolved issues

Chat conversation:
${chat}

Provide a clear, structured summary in 3-5 bullet points.`;

  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.flushHeaders();

  try {
    const model = genAI.getGenerativeModel({ model: 'gemini-3.6-flash' });
    const result = await model.generateContentStream(prompt);

    for await (const chunk of result.stream) {
      const text = chunk.text();
      if (text) {
        res.write(`data: ${JSON.stringify({ text })}\n\n`);
      }
    }

    res.write('data: [DONE]\n\n');
    res.end();
  } catch (error) {
    console.error('[AI] Summarize error:', error);
    res.write(`data: ${JSON.stringify({ error: 'AI service error. Please try again.' })}\n\n`);
    res.end();
  }
});

// ─── POST /api/ask ───────────────────────────────────────────────────────────
// Body: { messages: Message[], question: string }
// Response: { answer: string, messageId: string | null, excerpt: string | null }
router.post('/ask', async (req, res) => {
  const { messages, question } = req.body;
  if (!messages || !question) {
    return res.status(400).json({ error: 'messages and question are required' });
  }

  const chat = messages
    .map((m, i) => `[MSG_${i}|${m.id}] [${m.senderName}]: ${m.content}`)
    .join('\n');

  const prompt = `You are a helpful assistant that answers questions about chat conversations.
Answer the question based on the chat below. If the answer references a specific message, include its ID.

Chat (format: [MSG_index|message_id] [sender]: content):
${chat}

Question: ${question}

Respond in this exact JSON format:
{
  "answer": "Your answer here",
  "messageId": "the-message-id-if-referenced-or-null",
  "excerpt": "the relevant message excerpt or null"
}

Return ONLY valid JSON, no markdown.`;

  try {
    const model = genAI.getGenerativeModel({ model: 'gemini-3.6-flash' });
    const result = await model.generateContent(prompt);
    const text = result.response.text().trim();

    // Clean markdown code fences if present
    const cleaned = text.replace(/^```json\n?/, '').replace(/\n?```$/, '');
    const parsed = JSON.parse(cleaned);
    res.json(parsed);
  } catch (error) {
    console.error('[AI] Ask error:', error);
    res.status(500).json({ error: 'AI service error. Please try again.' });
  }
});

// ─── POST /api/extract-tasks ─────────────────────────────────────────────────
// Body: { messages: Message[] }
// Response: { tasks: [{ title, owner, dueDate, sourceMessageId }] }
router.post('/extract-tasks', async (req, res) => {
  const { messages } = req.body;
  if (!messages || messages.length === 0) {
    return res.status(400).json({ error: 'No messages provided' });
  }

  const chat = messages
    .map(m => `[${m.id}] [${m.senderName}]: ${m.content}`)
    .join('\n');

  const prompt = `You are a task extraction assistant. Analyze this chat conversation and extract any tasks, action items, or commitments.

Chat (format: [message_id] [sender]: content):
${chat}

Extract tasks and return them in this exact JSON format:
{
  "tasks": [
    {
      "title": "Clear task description",
      "owner": "Person responsible (or null if unclear)",
      "dueDate": "Due date in YYYY-MM-DD format (or null if not mentioned)",
      "sourceMessageId": "the message_id that mentioned this task"
    }
  ]
}

Rules:
- Only extract concrete tasks with clear action items
- If no tasks found, return { "tasks": [] }
- Return ONLY valid JSON, no markdown`;

  try {
    const model = genAI.getGenerativeModel({ model: 'gemini-3.6-flash' });
    const result = await model.generateContent(prompt);
    const text = result.response.text().trim();

    const cleaned = text.replace(/^```json\n?/, '').replace(/\n?```$/, '');
    const parsed = JSON.parse(cleaned);
    res.json(parsed);
  } catch (error) {
    console.error('[AI] Extract tasks error:', error);
    res.status(500).json({ error: 'AI service error. Please try again.' });
  }
});

module.exports = router;
