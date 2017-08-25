from py4j.java_gateway import JavaGateway
from py4j.java_gateway import GatewayParameters

'''
Hi! You can use this code as a template to create your own bot.  Also if you don't mind writing a blurb
about your bot's strategy you can put it as a comment here. I'd appreciate it, especially if I can help
debug any runtime issues that occur with your bot.
'''

# Optional Information. Fill out only if you wish.

# Your real name:
# Contact Email:
# Can this bot's code be shared publicly (Default: No):
# Can non-tournment gameplay of this bot be displayed publicly (Default: No):

myPort = 25368

try:
	with open("port.txt", "r") as portFile:
		myPort = int(portFile.readline())
except ValueError:
	print("Failed to parse port file! Will proceed with hard-coded port number.")
except:
	pass

print("Connecting to Java Gateway on port " + str(myPort))
gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True, port=myPort))
javaAgent = gateway.entry_point.getAgent()

class agent:

	def __init__(self, team):
		self.team = team # use self.team to determine what team you are. I will set to "blue" or "orange"

	def get_bot_name(self):
		# This is the name that will be displayed on screen in the real time display!
		return "TareBot"

	def get_output_vector(self, input):
		# Call the java process to get the output
		listOutput = javaAgent.getOutputVector([list(input[0]), list(input[1])], self.team)
		# Convert to a regular python list
		return list(listOutput)
