
from __future__ import print_function

import sys
import time

from qeo import EventReader as Reader, EventWriter as Writer

from ChatTypes import *

    
def on_receive(msg):
    print( "(%s) %s: %s" % (msg.chatbox, msg.sender, msg.msg))
    if msg.msg=='bye':
        print( "chat session closed by peer %s" % msg.sender)
        
def no_more():
    print( "end of data")     
    
def chat(writer):    
    room = None
    sender = None
    run = True
    while run:
        msg = raw_input()
        if msg.startswith("room="):
            room=msg[5:]
        elif msg.startswith("sender="):
            sender=msg[7:]
        elif msg=='state':
            d = ChatMessage(chatbox=room, sender=sender)
            print( "room=%s, sender=%s" % (d.chatbox, d.sender))
        elif msg!='':
            d = ChatMessage(chatbox=room, sender=sender, msg=msg)
            writer.write(d)
            if msg=='bye':
                #wait a bit, so the written msg can be received.
                time.sleep(0.2)
                run = False

def main(args):
    print( "Python Qeo chat app")
    with Reader(ChatMessage, on_receive, onNoMoreData=no_more):
        with Writer(ChatMessage) as writer:
            chat(writer)
    print( "chat application closed")

if __name__=="__main__":
    sys.exit(main(sys.argv))
    