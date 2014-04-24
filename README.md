MySQL Backup-Restore Java API
=============================

Started: 2014-04-23

Status: Prototype

Latest Version: 0.0.1-SNAPSHOT

Description: Java API to backup and restore a MySQL database.

Requirements
------------

 * Configurable
   * Connection String
   * Username
   * Password
   * Schema name
   * MySQL bin location
     * Backup: mysqldump.exe (win) / mysqldump (linux)
     * Restore: mysql.exe (win) / mysql (linux)
   * Backup directory
 * Backup to file
   * Specify filename
 * Restore from file
