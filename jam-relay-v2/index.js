const http = require('http');
const { Server } = require('socket.io');

const rooms = {};

const httpServer = http.createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      rooms: Object.keys(rooms).length,
      connections: io.engine.clientsCount,
    }));
    return;
  }
  res.writeHead(200);
  res.end('Echofy Jam Relay');
});

const io = new Server(httpServer, {
  cors: { origin: '*' },
  allowEIO3: true,
  pingTimeout: 30000,
  pingInterval: 10000,
});

io.on('connection', (socket) => {
  console.log('[CONNECT] ' + socket.id);

  socket.on('join_room', (data) => {
    const roomId = data.roomId;
    const roomCode = data.roomCode || '';
    const name = data.displayName || 'Anon';
    const role = data.role || 'GUEST';
    const participantId = data.participantId || socket.id;

    if (!roomId) {
      socket.emit('join_error', { reason: 'Missing room ID' });
      return;
    }

    socket.participantId = participantId;
    socket.roomId = roomId;
    socket.role = role;
    socket.displayName = name;

    if (role === 'HOST') {
      rooms[roomId] = {
        roomId,
        roomCode,
        hostSocketId: socket.id,
        hostParticipantId: participantId,
        hostName: name,
        allowGuestControls: true,
        latestPlayback: null,
        createdAt: Date.now(),
        participants: new Map(),
      };
    }

    const room = rooms[roomId];
    if (!room) {
      console.log('[JOIN_REJECTED] ' + name + ' -> room ' + roomId + ' (room does not exist)');
      socket.emit('join_error', {
        reason: 'Room not found. Make sure the host has started the session and check the room code.',
      });
      return;
    }

    room.participants.set(participantId, {
      participantId,
      displayName: name,
      role,
      joinedAtEpochMs: Date.now(),
      lastSeenAtEpochMs: Date.now(),
    });

    socket.join(roomId);
    console.log('[JOIN] ' + name + ' (' + role + ') -> room ' + roomId);

    socket.emit('join_ack', buildRoomState(room));
    io.in(roomId).emit('participants_updated', participantsArray(room));
    io.in(roomId).emit('room_state', buildRoomState(room));

    if (role !== 'HOST') {
      socket.to(roomId).emit('participant_joined', room.participants.get(participantId));
    }
  });

  socket.on('player_event', (data) => {
    const roomId = data.roomId;
    if (!roomId || !rooms[roomId]) return;

    const room = rooms[roomId];
    const senderId = data.issuedByParticipantId || socket.participantId;
    const sender = room.participants.get(senderId);
    const senderIsHost = sender?.role === 'HOST' || room.hostParticipantId === senderId;

    if (!senderIsHost && !room.allowGuestControls) {
      socket.emit('player_rejected', { reason: 'Host disabled guest controls' });
      return;
    }

    const event = {
      ...data,
      roomId,
      issuedByParticipantId: senderId,
      updatedAtEpochMs: data.updatedAtEpochMs || Date.now(),
      stateVersion: data.stateVersion || Date.now(),
    };

    room.latestPlayback = event;
    if (sender) sender.lastSeenAtEpochMs = Date.now();

    console.log(
      '[PLAYER_EVENT] room=' + roomId +
      ' from=' + (senderId || 'unknown') +
      ' media=' + event.mediaId +
      ' state=' + event.playbackState
    );

    socket.to(roomId).emit('player_event', event);
  });

  socket.on('guest_controls', (data) => {
    const roomId = data.roomId;
    const room = rooms[roomId];
    if (!room) return;
    if (room.hostParticipantId !== socket.participantId) return;

    room.allowGuestControls = Boolean(data.allowGuestControls);
    console.log('[GUEST_CONTROLS] room=' + roomId + ' enabled=' + room.allowGuestControls);

    io.in(roomId).emit('room_meta', buildRoomState(room));
    io.in(roomId).emit('room_state', buildRoomState(room));
  });

  socket.on('leave_room', (roomId) => {
    console.log('[LEAVE] ' + socket.id + ' left room ' + roomId);
    leaveRoom(socket, roomId, 'Host ended the session');
  });

  socket.on('disconnect', (reason) => {
    console.log('[DISCONNECT] ' + socket.id + ' reason=' + reason);
    if (socket.roomId) {
      leaveRoom(socket, socket.roomId, 'Host disconnected');
    }
  });
});

setInterval(() => {
  const now = Date.now();
  const staleThreshold = 4 * 60 * 60 * 1000;
  for (const roomId of Object.keys(rooms)) {
    if (now - rooms[roomId].createdAt > staleThreshold) {
      console.log('[CLEANUP] Removing stale room ' + roomId);
      io.in(roomId).emit('room_closed', { reason: 'Session expired' });
      delete rooms[roomId];
    }
  }
}, 5 * 60 * 1000);

function leaveRoom(socket, roomId, hostReason) {
  const room = rooms[roomId];
  socket.leave(roomId);
  if (!room) return;

  if (room.hostSocketId === socket.id) {
    console.log('[ROOM_CLOSED] ' + roomId);
    io.in(roomId).emit('room_closed', { reason: hostReason });
    delete rooms[roomId];
    return;
  }

  room.participants.delete(socket.participantId);
  socket.to(roomId).emit('participant_left', {
    participantId: socket.participantId,
  });
  io.in(roomId).emit('participants_updated', participantsArray(room));
  io.in(roomId).emit('room_state', buildRoomState(room));
}

function buildRoomState(room) {
  return {
    roomId: room.roomId,
    roomCode: room.roomCode,
    hostParticipantId: room.hostParticipantId,
    hostName: room.hostName,
    allowGuestControls: room.allowGuestControls,
    createdAt: room.createdAt,
    serverTimeMs: Date.now(),
    participants: participantsArray(room),
    latestPlayback: room.latestPlayback,
    playback: room.latestPlayback,
  };
}

function participantsArray(room) {
  return Array.from(room.participants.values());
}

const PORT = process.env.PORT || 3000;
httpServer.listen(PORT, '0.0.0.0', () => {
  console.log('Echofy Jam Relay v2 running on port ' + PORT);
});
