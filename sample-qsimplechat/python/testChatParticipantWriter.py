'''
Created on Nov 27, 2012

@author: jasm
'''

import sys
import readline


# Qeo imports:
from qeo import StateWriter, StateReader


# Application specific imports:
from ChatTypes import ChatParticipant

def onData(sample):
    print "onData called: %s - %s - %s" % (sample.name, sample.room, sample.state)

def onDispose(sample):
    print "onDispose called: %s - %s" % (sample.name, sample.room)
    
def onNoMoreData():
    print "onNoMoreData called"

def main():
    with StateReader(ChatParticipant, onData=onData, onNoMoreData=onNoMoreData, onDispose=onDispose) as reader:
        with StateWriter(ChatParticipant) as writer:
            s1 = ChatParticipant(name="me", room="kitchen", state="busy")
            d1 = ChatParticipant(name="me", room="kitchen")
            u1 = ChatParticipant(name="me", room="kitchen", state="away")
            
            s2 = ChatParticipant(name="you", room="terras", state="idle")
            d2 = ChatParticipant(name="you", room="terras")
            u2 = ChatParticipant(name="you", room="terras", state="away")
            while True:
                cmd = raw_input("Enter command: ")
                if   cmd == "s1": writer.write(s1)
                elif cmd == "s2": writer.write(s2)
                elif cmd == "u1": writer.write(u1)
                elif cmd == "u2": writer.write(u2)
                elif cmd == "d1": writer.remove(d1)
                elif cmd == "d2": writer.remove(d2)
                elif cmd == "q": break

            writer.remove(d1)
            writer.remove(d2)
        

if __name__ == '__main__':
    sys.exit(main())