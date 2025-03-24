###############################################################################
#
# Makefile for a Java project
#
###############################################################################

# **** Why do we need this file? ****
# We want our users to have a simple API to run the JackCompiler, no matter the language
# it was written in. So, we need a "wrapper" that will hide all language-specific details to do so,
# thus enabling our users to simply type 'JackCompiler <path>' in order to use it.

# **** What are makefiles? ****
# This is a sample makefile. 
# The purpose of makefiles is to make sure that after running "make" your project is ready for execution.

# **** What should I change in this file to make it work with my project? ****
# Usually, Java projects need to compile all java source files and give execution permissions for your 
# run file executable to run. The executable for project 11 should be called JackCompiler.
# Obviously, your project may be more complicated and require a different makefile.
# IMPORTANT 1: For this file to run when you call "make", rename it from "Makefile-java" to "Makefile".
# IMPORTANT 2: If your project requires more than simply setting execution permissions, define rules
#			   accordingly.

# **** How are rules defined? ****
# The following line is a rule declaration: 
# all:
#	javac -encoding ISO-8859-1 *.java
#	chmod +x JackCompiler

# A makefile rule is a list of prerequisites (other rules that need to be run before this rule) and commands 
# that are run one after the other. The "all" rule is what runs when you call "make".
# In this example, all it does is compile all Java files in the current directory, and grant execution 
# permissions for your run time executable, so your project will be able to run on the graders' computers. 
# In this case, the "all" rule has no preqrequisites.

# A general rule looks like this:
# rule_name: prerequisite1 prerequisite2 prerequisite3 prerequisite4 ...
#	command1
#	command2
#	command3
#	...
# Where each preqrequisite is a rule name, and each command is a command-line command (for example chmod, 
# javac, echo, etc').

# **** Beginning of the actual Makefile ****
# The following line is a declaration of a variable named JAVAC:
JAVAC=javac
# As you can see, the variable only holds the name of the java compiler.

# The JAVACFLAGS variable should include any special flags your program needs for compilation:
JAVACFLAGS=-encoding ISO-8859-1

# The SRCS variable should include the filenames of all .java source files relevant for your project:
SRCS=*.java
# It is currently defined to be all .java files in the current directory.

# The EXEC variable should include the name of the run file executable only.
# In the case of Project 11, it is "JackCompiler":
EXEC=JackCompiler

# Let's define more variables that we'll use later on:
TAR=tar
TARFLAGS=cvf
TARNAME=projXXX.tar
TARSRCS=$(SRCS) $(EXEC) Makefile

# The following line is a rule declaration. A makefile rule is a list of prerequisites (other rules that 
# need to be run before this rule) and commands that are run one after the other. The "all" rule is what 
# runs when you call "make":
all: compile

# As you can see, the "all" rule requires the "compile" rule. Meaning, that when you call the 
# "all" rule, the "compile" rule is called too. 

# The "compile" rule performs a compilation of all java files specified in the SRCS variable
# and then gives execution permissions to the run file called JackCompiler
compile:
	$(JAVAC) $(JAVACFLAGS) $(SRCS)
	chmod +x $(EXEC)
	
# The "compile" rule simply runs two commands one after the other:
# 1. "$(JAVAC) $(JAVACFLAGS) $(SRCS)" - this command is constructed from 3 variables, and if you use 
# the defaults specified here it simply translated to "javac *.java", meaning that it compiles all 
# java files in the current directory.
# 2. "chmod +x $(EXEC)" - this command is constructed from "chmod +x", which is the terminal command 
# that grants execution permissions, and "$(EXEC)", which should contain the name of your run file. 
# This command simply gives execution permissions for your run file, so the graders could run it on their
# computers.

# The following rule allows you to call "make tar". It will put all the files specified
# in the TARSRCS variable in a tar. This is for your convenience only and you don't have to support it.
tar:
	$(TAR) $(TARFLAGS) $(TARNAME) $(TARSRCS)

# The following rules allows you to call "make clean". It will remove all compiled class files. 
# This is for your convenience only and you don't have to support it.	
clean:
	rm -f *.class *~

