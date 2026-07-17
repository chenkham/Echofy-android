sudo dnf module enable nodejs:20 -y
sudo dnf install nodejs -y
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --reload
npm init -y
npm install socket.io
sudo npm install -g pm2
pm2 start jam-server.js
