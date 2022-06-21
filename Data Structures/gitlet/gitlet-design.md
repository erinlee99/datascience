# Gitlet Design Document

**Name**: Erin Lee

## Classes and Data Structures

### Commit

#### Instance Variables
* Message - contains the message of a commit
* Timestamp - time at which a commit was created. Assigned by constructor
* Parent - the parent commit of a commit object

This class is a HashMap that stores all contents of files.

### Blob

This class stores the contents of each file.

**Fields**
String name - the name of the file 
String content - the  contents of the file 
String hash - the hash string of the file


### Gitlet

This is a class that keeps track of all files and commits.

**Fields**
ArrayList tracked - tracked files for this round of commit
ArrayList untracked - files that are untracked for the current round of stage
ArrayList commits - list of the hash values of all commits
ArrayList branches - list of all branches
HashMap branchHeads - heads of all branched
HashMap remoteRepos - names and locations of all remote repositories


## Algorithms

### Blob

getName() - gets the name (string) of the file 
getContentsAsByte() - gets the contents of the file as a byte array
getContentsAsString() - gets the contents of the file as a string 
getHash() - gets the hash string of a file

### Commit 

getContents() gets the contents of the file
getBlobs() gets the blobs of the commit 
getHash() - gets the hash value of the file
getBranch() - gets the value of the branch of the file
getMessage() - gets the message of the commit as a string
getParent() - gets the parent of the commit of the file

### Gitlet

init() - initializes the Gitlet directory
run() - runs the various functions of Gitlet
add() - adds the file to the staging area
remove() - removes the file from the staging area and from the the directory if it is a tracked file
commit() - creates a commit object from the tracked array list and the staging area 
addbranch() - creates a new branch of commits
removebranch() - removes branch
gethead() - returns the head of the current Gitlet
merge() - combines two branches into a new commit
push() - pushes the associated commits to that remote repository after checking to see if the head of the commit exists in a remote repository
fetch() - retrieves a file from the passed remote repository and adds the content of a file to its own branch
pull() - calls fetch() & merge() on the selected files


## Persistence

Storing the contents of files is necessary in Gitlet.
All files are all stored according to hash value, and are thus stored in the .gitlet folder with their hash value as their name. 
The relevant information of the files is stored within their serialized numbers or the gitlet object. 
As long as the values are stored in the github directory, the contents of the files will not be changed by any function because the hashvalues of the contents are immutable and thus accurate. 

