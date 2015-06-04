'''
Created on Nov 27, 2012

@author: smoutj
'''

from qeo import Data, Type, String, Bytes

__all__ = [
           "ChatMessage",
           "ChatParticipant",
           ]

@Type('com.technicolor.demo.chat.%')
class ChatMessage(Data):
    chatbox = String(defvalue='DDS')
    sender = String(name='from', defvalue='python')
    msg = String(name='message')

@Type('com.technicolor.demo.chat.%')
class ChatParticipant(Data):
    state = String(defvalue='Idle')
    name = String(defvalue='<unknown>', key=True)
    room = String(defvalue='<unknown>', key=True)
    avatar = Bytes()