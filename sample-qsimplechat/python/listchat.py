
import qeo

import ChatTypes

def main():
    reader = None
    try:
        reader = qeo.StateReader(ChatTypes.ChatParticipant)
        changed = False
        while True:
            if changed:
                print "=== CHATTERS "+"="*62
                for m in reader.states():
                    print "%s, room=%s, state=%s" % (m.name, m.room, m.state)
                print "="*75
                changed = False
            reader.wait(10)
            while True:
                sample, info = reader.read()
                if sample is None:
                    # really nothing available
                    break
                changed = True
                if info.remvoed:
                    print "LEFT:", sample.name, sample.room
                elif info.valid:
                    print "UPDATE:", sample.name, sample.room, sample.state
                #else: someone dropped of
             
    finally:
        reader.close()
    

if __name__=='__main__':
    main()