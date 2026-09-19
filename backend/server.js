require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const aiRoutes = require('./routes/ai');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

app.use(cors());
app.use(express.json());
app.use('/api', aiRoutes);

// ─── In-memory state ────────────────────────────────────────────────────────
const rooms = {};      // roomId -> { messages: [], users: {} }

function getOrCreateRoom(roomId) {
  if (!rooms[roomId]) {
    rooms[roomId] = { messages: [], users: {} };
  }
  return rooms[roomId];
}

// ─── Socket.IO events ────────────────────────────────────────────────────────
io.on('connection', (socket) => {
  console.log(`[Socket] Connected: ${socket.id}`);

  // Join a room
  socket.on('join', ({ roomId, userId, userName }) => {
    // Keep track of which room and user this socket represents
    socket.roomId = roomId;
    socket.userId = userId;
    
    socket.join(roomId);
    const room = getOrCreateRoom(roomId);
    room.users[userId] = userName;

    console.log(`[Socket] ${userName} joined room ${roomId}`);

    // Notify others in the room
    socket.to(roomId).emit('user_joined', {
      userId,
      userName,
      timestamp: Date.now()
    });

    // Send recent message history (last 50)
    const history = room.messages.slice(-50);
    socket.emit('history', history);

    // Broadcast updated user list
    io.to(roomId).emit('user_list', Object.entries(room.users).map(([id, name]) => ({ id, name })));
  });

  // Send a message
  socket.on('send_message', ({ roomId, message }) => {
    const room = getOrCreateRoom(roomId);

    // Deduplication: skip if we've already seen this message ID
    const isDuplicate = room.messages.some(m => m.id === message.id);
    if (isDuplicate) {
      console.log(`[Socket] Duplicate message ignored: ${message.id}`);
      return;
    }

    const enriched = {
      ...message,
      serverTimestamp: Date.now()
    };

    room.messages.push(enriched);

    // Keep only the last 200 messages in memory
    if (room.messages.length > 200) {
      room.messages.shift();
    }

    // Broadcast to all in room including sender
    io.to(roomId).emit('new_message', enriched);
    console.log(`[Socket] Message in ${roomId} from ${message.senderName}: ${message.content.substring(0, 50)}`);
  });

  // Typing indicator
  socket.on('typing', ({ roomId, userId, userName, isTyping }) => {
    socket.to(roomId).emit('typing', { userId, userName, isTyping });
  });

  // Leave room
  socket.on('leave_room', ({ roomId, userId }) => {
    const room = rooms[roomId];
    if (room) {
      const userName = room.users[userId];
      delete room.users[userId];

      socket.to(roomId).emit('user_left', { userId, userName, timestamp: Date.now() });
      io.to(roomId).emit('user_list', Object.entries(room.users).map(([id, name]) => ({ id, name })));
    }
    socket.leave(roomId);
    console.log(`[Socket] ${userId} left room ${roomId}`);
  });

  socket.on('disconnect', () => {
    console.log(`[Socket] Disconnected: ${socket.id}`);
    
    // Automatically remove user if they disconnected without explicitly leaving
    if (socket.roomId && socket.userId) {
      const room = rooms[socket.roomId];
      if (room && room.users[socket.userId]) {
        const userName = room.users[socket.userId];
        delete room.users[socket.userId];
        
        io.to(socket.roomId).emit('user_left', { userId: socket.userId, userName, timestamp: Date.now() });
        io.to(socket.roomId).emit('user_list', Object.entries(room.users).map(([id, name]) => ({ id, name })));
        
        console.log(`[Socket] Removed ${socket.userId} from room ${socket.roomId} due to disconnect`);
      }
    }
  });
});

// ─── Health check ────────────────────────────────────────────────────────────
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: Date.now() });
});

// ─── Start server ─────────────────────────────────────────────────────────────
const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`AIChat backend running on http://0.0.0.0:${PORT}`);
});
