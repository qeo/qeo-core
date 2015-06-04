'''
Created on Nov 27, 2012

@author: jasm
'''

import sys

from PyQt4.QtGui import QMainWindow, QApplication, QGridLayout, QWidget, QTextEdit, QLabel, QListWidget, QPushButton
from PyQt4.QtCore import SIGNAL, Qt as Qt

from qeo import StateReader, EventReader


# Application specific imports:
from ChatTypes import *

class ChatTracer(QMainWindow):
    def __init__(self):
        super(ChatTracer, self).__init__()
        self.__initGui()
        self.__initQeo()
        self.connect(self, SIGNAL("newLogMsg"), self.__appendLog)

    def __initGui(self):
        self.setWindowTitle("ChatTracer")

        g = QGridLayout()
        w = QWidget()
        w.setLayout(g)
        self.setCentralWidget(w)

        self.logWindow = QTextEdit()
        self.logWindow.setReadOnly(True)
        self.logWindow.setFocusPolicy(Qt.NoFocus)
        self.logWindow.setMinimumWidth(300)
        g.addWidget(self.logWindow, 0, 0, 3, 1)

        l = QLabel("ChatParticipants")
        g.addWidget(l, 0, 1)

        self.participants = QListWidget()
        self.participants.setMaximumWidth(250)
        self.participants.setFocusPolicy(Qt.NoFocus)
        g.addWidget(self.participants, 1, 1)

        pb = QPushButton("Refresh Participants")
        pb.clicked.connect(self.onRefreshParticipants)
        g.addWidget(pb, 2, 1)

        g.setColumnStretch(0, 3)
        g.setColumnStretch(1, 2)

        self.show()
        pb.setFocus()

    def __initQeo(self):
        self.participantReader = StateReader(
                ChatParticipant,
                onData=self.forwardData,
                onNoMoreData=lambda cls="ChatParticipant": self.forwardNoMoreData(cls),
                onDispose=self.forwardDispose)
        self.msgReader = EventReader(ChatMessage,
                onData=self.forwardData,
                onNoMoreData=lambda cls="ChatMessage": self.forwardNoMoreData(cls))

        self.connect(self, SIGNAL("onData"), self.onData)
        self.connect(self, SIGNAL("onNoMoreData"), self.onNoMoreData)
        self.connect(self, SIGNAL("onDispose"), self.onDispose)


    def closeApp(self):
        self.participantReader.close()
        self.participantReader = None
        self.msgReader.close()
        self.msgReader = None

    def __appendLog(self, msg):
        self.logWindow.append(msg)

    def __buildLogMsg(self, prefix, data):
        msg = [prefix + ": "]
        if data:
            msg.append("%s" % data.__class__.__name__)
            msg.append("{")
            for k, v in data.__dict__.iteritems():
                msg.append("%s='%s'" % (str(k), str(v)))
        msg.append("}")
        self.__appendLog(' '.join(msg))

    def forwardData(self, s):
        self.emit(SIGNAL("onData"), s)

    def forwardDispose(self, s):
        self.emit(SIGNAL("onDispose"), s)

    def forwardNoMoreData(self, s):
        self.emit(SIGNAL("onNoMoreData"), s)

    def onData(self, sample):
        self.__buildLogMsg("onData", sample)

    def onDispose(self, sample):
        print "onDispose called: %s - %s" % (sample.name, sample.room)

    def onNoMoreData(self, class_):
        self.__buildLogMsg("onNoMoreData", None)

    def onRefreshParticipants(self):
        self.participants.clear()
        for s in self.participantReader.states():
            self.participants.addItem("%s - %s - %s" % (s.name, s.room, s.state))

def main(args):
    app = QApplication(args)
    win = ChatTracer()
    app.lastWindowClosed.connect(win.closeApp)
    return app.exec_()


if __name__ == '__main__':
    sys.exit(main(sys.argv))
