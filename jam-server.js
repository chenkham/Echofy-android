const io = require("socket.io")(3000, {
  cors: {
    origin: "*",
  }
});

io.on("connection", (socket) => {
  console.log(`User connected: ${socket.id}`);

  // User joins a Listen Together room
  socket.on("join_room", (roomId) => {
    socket.join(roomId);
    console.log(`User ${socket.id} joined room: ${roomId}`);
  });

  // User leaves a room
  socket.on("leave_room", (roomId) => {
    socket.leave(roomId);
    console.log(`User ${socket.id} left room: ${roomId}`);
  });

  // Relay playback events (play, pause, seek, next song) to others in the room
  socket.on("player_event", (data) => {
    if (data && data.roomId) {
      // socket.to() broadcasts to everyone in the room EXCEPT the sender
      socket.to(data.roomId).emit("player_event", data);
    }
  });

  // Relay profile details (Name initials, etc)
  socket.on("user_state", (data) => {
    if (data && data.roomId) {
      socket.to(data.roomId).emit("user_state", data);
    }
  });

  socket.on("disconnect", () => {
    console.log(`User disconnected: ${socket.id}`);
  });
});

console.log("Echofy Jam WebSocket server running on port 3000");
