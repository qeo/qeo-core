#!/usr/bin/env python

import sys
import os


from PyQt4.QtGui import QMainWindow, QApplication, QHBoxLayout, QPushButton, QWidget, \
                         QLineEdit, QLabel, QTextEdit, QGridLayout, QListWidget, QGroupBox, \
                         QRadioButton, QColor, QBrush, QPixmap, QIcon
from PyQt4.QtCore import QString, SIGNAL, Qt as Qt, QSize


from qeo import EventReader, EventWriter, StateReader, StateWriter


from ChatTypes import *


class ChatWindow(QMainWindow):
    _cmdChar = '!'

    def __init__(self):
        super(ChatWindow, self).__init__()

        self.initUI()
        self.initQeo()
        self.initChat()

    def initUI(self):
        self.setWindowTitle("PyQt Chat")

        widget = QWidget()
        self.setCentralWidget(widget)

        grid = QGridLayout()
        widget.setLayout(grid)

        self.msgOutput = QTextEdit()
        self.msgOutput.setReadOnly(True)
        self.msgOutput.setFocusPolicy(Qt.NoFocus)
        self.msgOutput.setMinimumWidth(300)
        grid.addWidget(self.msgOutput, 0, 0, 5, 1)

        lbl = QLabel("Rooms:")
        grid.addWidget(lbl, 0, 1)
        self.roomList = QListWidget()
        self.roomList.setMaximumWidth(250)
        self.roomList.setSortingEnabled(True)
        self.roomList.itemSelectionChanged.connect(self.newRoomSelected)
        grid.addWidget(self.roomList, 1, 1)

        lbl = QLabel("Users in the room:")
        grid.addWidget(lbl, 2, 1)
        self.userList = QListWidget()
        self.userList.setFocusPolicy(Qt.NoFocus)
        self.userList.setMaximumWidth(250)
        self.userList.setSortingEnabled(True)
        self.userList.setIconSize(QSize(100, 100))
        grid.addWidget(self.userList, 3, 1)

        gb = QGroupBox("User State:")
        hbox = QHBoxLayout()
        for state in ["Idle", "Busy", "Away"]:
            rb = QRadioButton(state)
            if state == "Idle":
                rb.setChecked(True)
            rb.clicked.connect(lambda x, y=state: self.updateMyParticipantState(state=y))
            hbox.addWidget(rb)
        gb.setLayout(hbox)
        grid.addWidget(gb, 4, 1)

        self.msgEntry = QLineEdit()
        self.msgEntry.returnPressed.connect(self.userMessageEntered)
        sendBtn = QPushButton("Send")
        sendBtn.clicked.connect(self.userMessageEntered)
        msgEntryLbl = QLabel(text=QString("Message:"))

        hbox = QHBoxLayout()
        hbox.addWidget(msgEntryLbl)
        hbox.addWidget(self.msgEntry)
        hbox.addWidget(sendBtn)

        grid.addLayout(hbox, 5, 0, 1, 2)

        grid.setColumnStretch(0, 2)
        grid.setColumnStretch(1, 1)

        self.show()
        self.msgEntry.setFocus()


    def initQeo(self):
        self.chatMsgReader = EventReader(ChatMessage, onData=self.forwardChatMsgData)
        self.chatMsgWriter = EventWriter(ChatMessage)
        self.chatParticipantReader = StateReader(ChatParticipant, onData=self.forwardChatParticipantData, onRemove=self.forwardChatParticipantDispose)
        self.chatParticipantWriter = StateWriter(ChatParticipant)

        # Using the below signals (and accompanying slots), we forward the received samples from
        # the Qeo Reader thread to the Qt GUI thread. This is not a Qeo limitation, but a Qt requirement.
        self.connect(self, SIGNAL("chatParticipantData"), self.onChatParticipantData)
        self.connect(self, SIGNAL("chatParticipantDispose"), self.onChatParticipantDispose)
        self.connect(self, SIGNAL("chatMessageData"), self.onChatMessageData)

    def cleanupQeo(self):
        # TODO
        # Q: Should I call xxx.remove() first (and wait a bit so that the remove has a chance to reach
        #    other destinations?
        self.chatMsgReader.close()
        self.chatMsgReader = None
        self.chatMsgWriter.close()
        self.chatMsgWriter = None
        self.chatParticipantReader.close()
        self.chatParticipantReader = None
        self.chatParticipantWriter.close()
        self.chatParticipantWriter = None

    def initChat(self):
        self.myState = ChatParticipant()

        self.roomChangeStarted = True

        self.__cmds = {
                       "help": ["Show this list of available commands", self.handleHelpCommand],
                       "user": ["Show/Change your current user name", self.handleUserCommand],
                       "users": ["Show a list of all available users in the current room", self.handleUsersCommand],
                       "room": ["Show/Change your current room", self.handleRoomCommand],
                       "rooms": ["Show a list of all discovered rooms", self.handleRoomsCommand],
                       "bye": ["Exit the chat application", self.handleByeCommand],
                       "avatar": ["Load the given image file as your avatar", self.handleAvatarCommand],
                       }

        # Initialize user and room using the internal commands
        self.msgEntry.setText("!avatar some-image.jpg")
        self.userMessageEntered()
        self.msgEntry.setText("!room Qeo")
        self.userMessageEntered()
        self.msgEntry.setText("!user %s-%d" % (os.environ["USER"], os.getpid()))
        self.userMessageEntered()
        self.msgEntry.setText("!help")
        self.userMessageEntered()
        self.msgEntry.clear()


    def updateMyParticipantState(self, name=None, room=None, state=None, avatar=None):
        doWrite, doDelete = False, False
        origState = ChatParticipant(name=self.myState.name,
                                    room=self.myState.room,
                                    state=self.myState.state,
                                    avatar = self.myState.avatar)

        if name is not None and self.myState.name != name:
            self.myState.name = name
            doWrite = True
            doDelete = True

        if room is not None and self.myState.room != room:
            self.myState.room = room
            doWrite = True
            doDelete = True
            self.roomChangeStarted = True

        if state is not None and self.myState.state != state:
            self.myState.state = state
            doWrite = True

        if avatar is not None:
            # Note: at this point in time, the avatar is assumed to be a filename referring to
            # a picture
            with open(avatar) as f:
                tmp = bytearray(f.read())
                if len(tmp) > 0:
                    self.myState.avatar = tmp
                    doWrite = True

        # Note: only start writing/disposing when the key (name and room) is fully defined
        if doDelete and origState.name != "<unknown>" and origState.room != "<unknown>":
            self.chatParticipantWriter.remove(origState)

        if doWrite and self.myState.name != "<unknown>" and self.myState.room != "<unknown>":
            self.chatParticipantWriter.write(self.myState)

    def onChatMessageData(self, chatMsg):
        if chatMsg.chatbox == self.myState.room:
            self.msgOutput.append(QString("%s : %s" % (chatMsg.sender, chatMsg.msg)))

    def forwardChatParticipantData(self, s):
        self.emit(SIGNAL("chatParticipantData"), s)

    def onChatParticipantData(self, s):
        print "onChatParticipantData: %s - %s - %s" % (s.name, s.room, s.state)
        if s.name == self.myState.name:
            if self.roomChangeStarted:
                self.updateRoomList(room=s.room, add=True)
                self.roomChangeStarted = False
                self.refreshUserList(s.room)
            self.updateUserList(s.name, s.state, s.avatar, add=True)
        else:
            if s.room == self.myState.room:
                self.updateUserList(s.name, s.state, s.avatar, add=True)
            else:
                self.updateRoomList(room=s.room, add=True)
        self.msgOutput.append("| user '%s' lives in room '%s' and is in state '%s'" % (s.name, s.room, s.state))

    def forwardChatParticipantDispose(self, s):
        self.emit(SIGNAL("chatParticipantDispose"), s)

    def onChatParticipantDispose(self, s):
        print "onChatParticipantDispose: %s - %s" % (s.name, s.room)
        if s.room == self.myState.room:
            self.updateUserList(s.name, add=False)
        self.updateRoomList(s.room, add=False)
        self.msgOutput.append("| user '%s' left room '%s'" % (s.name, s.room))


    def forwardChatMsgData(self, chatMsg):
        """
        You *must* update the GUI (aka. any widget) only from within the GUI thread! Alas,
        self.onChatMsgData() will be called from within another thread as a result of the EventReader
        behavior.
        The solution to this is that we emit a (Python only) SIGNAL, which we have to connect to
        a SLOT - i.e. any Python callable.
        """
        if chatMsg:
            self.emit(SIGNAL("chatMessageData"), chatMsg)

    def newRoomSelected(self):
        ci = self.roomList.currentItem()
        if ci and ci.isSelected():
            self.handleRoomCommand([str(ci.text())])

    def userMessageEntered(self):
        txt = str(self.msgEntry.text())
        if txt:
            if not self.handleCommand(txt):
                msg = ChatMessage(chatbox=self.myState.room, sender=self.myState.name, msg=txt)
                self.chatMsgWriter.write(msg)
        self.msgEntry.selectAll()

    def updateRoomList(self, room=None, add=True):
        if add:
            rl = self.roomList.findItems(room, Qt.MatchFlags(Qt.MatchFixedString))
            if not rl:
                self.roomList.addItem(room)
            rl = self.roomList.findItems(room, Qt.MatchFlags(Qt.MatchFixedString))
            if room == self.myState.room:
                self.roomList.setItemSelected(rl[0], True)

        else:
            usersInRoom = False
            for s in self.chatParticipantReader.states():
                if s.room == room:  # and s.name != self.myState.name:
                    usersInRoom = True
                    break
            if not usersInRoom:
                rl = self.roomList.findItems(room, Qt.MatchFlags(Qt.MatchFixedString))
                if rl:
                    self.roomList.takeItem(self.roomList.row(rl[0]))

    def refreshUserList(self, room):
        """ Re-iterate over all users in that room """
        self.userList.clear()
        for s in self.chatParticipantReader.states():
            if s.room == room:
                self.updateUserList(s.name, s.state, s.avatar, add=True)

    def updateUserList(self, name, state=None, avatar=None, add=True):
        print "Updating userList",
        ul = self.userList.findItems("%s (" % name, Qt.MatchFlags(Qt.MatchStartsWith))
        if add:
            print "Adding user: %s" % name
            if ul:
                ul[0].setText("%s (%s)" % (name, state))
            else:
                self.userList.addItem("%s (%s)" % (name, state))

            ul = self.userList.findItems("%s (" % name, Qt.MatchFlags(Qt.MatchStartsWith))
            if name == self.myState.name:
                self.userList.setItemSelected(ul[0], True)

            if len(avatar):
                print "size/length of avatar for %s is %d" % (name, len(avatar))
                pm = QPixmap()
                pm.loadFromData(avatar)
                ul[0].setIcon(QIcon(pm))

            color = None
            if state.lower() == "idle":
                color = QColor(Qt.green)
            elif state.lower() == "busy":
                color = QColor(Qt.red)
            elif state.lower() == "away":
                color = QColor(Qt.yellow)
            if color:
                ul[0].setBackground(QBrush(color))

        else:
            print "Removing user: %s" % name
            if ul:
                self.userList.takeItem(self.userList.row(ul[0]))

    def handleCommand(self, txt):
        if not txt.startswith(self._cmdChar):
            return False

        self.msgOutput.append(txt)
        args = txt.split(' ')
        try:
            self.__cmds[args[0][1:]][1](args[1:])
        except KeyError:
            self.msgOutput.append("| Command '%s' is not available. Try '%shelp'." % (args[0], self._cmdChar))

        return True

    def handleAvatarCommand(self, args):
        if args:
            fn = ' '.join(args)
            self.msgOutput.append("| Loading new avatar from file '%s'" % fn)
            self.updateMyParticipantState(avatar=fn)
            self.msgOutput.append("| info: image size = %d" % len(self.myState.avatar))
        else:
            self.msgOutput.append("| Error: please provide a filename as argument")

    def handleHelpCommand(self, args):
        self.msgOutput.append("| Available commands")
        for cmd in sorted(self.__cmds.keys()):
            self.msgOutput.append("|    %s%s --> %s" % (self._cmdChar, cmd, self.__cmds[cmd][0]))

    def handleUserCommand(self, args):
        if args:
            self.updateMyParticipantState(name=' '.join(args))
            self.msgOutput.append("| Changed user name to '%s'" % self.myState.name)
        else:
            self.msgOutput.append("| Current user name is '%s'" % self.myState.name)

    def handleUsersCommand(self, args):
        self.msgOutput.append("List of users in the '%s' room:" % self.myState.room)
        users = []
        for s in self.chatParticipantReader.states():
            if s.room == self.myState.room:
                users.append(str(s.name))
        for u in sorted(users):
            self.msgOutput.append("|   - %s" % u)

    def handleRoomCommand(self, args):
        if args:
            self.updateMyParticipantState(room=' '.join(args))
            self.msgOutput.append("|----------------------------------------")
            self.msgOutput.append("| Entering room '%s'" % self.myState.room)
        else:
            self.msgOutput.append("| Current room is '%s'" % self.myState.room)

    def handleRoomsCommand(self, args):
        self.msgOutput.append("List of discovered rooms:")
        rooms = []
        for s in self.chatParticipantReader.states():
            if not s.room in rooms:
                rooms.append(str(s.room))
        for r in sorted(rooms):
            self.msgOutput.append("|    - %s" % r)

    def handleByeCommand(self, args):
        QApplication.closeAllWindows()



def main(args):
    app = QApplication(args)
    win = ChatWindow()
    app.lastWindowClosed.connect(win.cleanupQeo)
    sys.exit(app.exec_())


if __name__ == '__main__':
    main(sys.argv)
