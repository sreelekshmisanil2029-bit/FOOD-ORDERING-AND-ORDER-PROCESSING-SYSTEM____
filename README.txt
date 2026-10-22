FOOD ORDERING AND ORDER PROCESSING SYSTEM
========================================

FILES
-----
1. FoodOrderingServer.java - Java server, menu and order-processing logic
2. FoodOrder.java          - Java order/model classes
3. index.html              - Web page and JavaScript interactions
4. style.css               - Website design
5. README.txt              - Instructions

TECHNOLOGIES
------------
Java, HTML, CSS, JavaScript, Queue (FIFO), ArrayList, built-in Java HTTP server.

REQUIREMENTS
------------
Install Java 17 or newer. No Maven or external Java libraries are required.

HOW TO RUN
----------
1. Put all five files in the same folder.
2. Open Command Prompt / Terminal in that folder.
3. Compile both Java files:

   javac FoodOrder.java FoodOrderingServer.java

4. Start the server:

   java FoodOrderingServer

5. Open this address in your browser:

   http://localhost:8080

Do not open index.html by double-clicking it. Open it through the localhost address
after starting the Java server.

FEATURES
--------
- Displays food menu and prices
- Lets a customer select food and quantity
- Calculates the total bill
- Generates an order ID
- Adds orders to a FIFO queue
- Processes the oldest pending order first
- Shows pending orders and order history
- Displays order details and dashboard summary

JAVA CONCEPTS
-------------
- Classes and objects: FoodOrder and FoodOrder.Line
- Encapsulation
- ArrayList and Queue
- LinkedList as the FIFO queue implementation
- Methods and exception handling
- HTTP request handling with Java's built-in HttpServer

IMPORTANT
---------
This is a simple college-project prototype. Orders are stored in memory and are
cleared when the server stops. It does not process real payments.
