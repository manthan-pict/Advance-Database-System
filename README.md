# MiniBase

This is Database system with customized advance operators such as - 
different implementation of skyline operation - which returns the interesting tuple from the given dataset. (reference - ) 
1. Nested Loop Skyline
2. Block Nested Loop Skyline
3. Sorted First Skyline
4. Btree skyline 
5. Btree sorted skyline 

Different implementaion of Top K Join - returns the top k tuples from the given two relations on selected join attribute with sum of merge attribute as the function to decide the top k tuples. speciality of the top k join is that top k tuples are found without entirely joining the two tables.
1. Hash based top k join
2. NRA (No Random Access) based top k join.

Along with this all the normal oprators of DBMS system are also implemented.

Driver file - phase3driver.java

Donot push any change regarding make file. It will affect other people make file as well(JDK and LIB Path). Strictly Java. If any makefile change is required directly make that change on the remote git and let others know in advance.
