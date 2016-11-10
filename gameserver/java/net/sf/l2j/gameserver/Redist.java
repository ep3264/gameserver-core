/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author user
 *
 */
public class Redist
{
	private static final String url = "jdbc:mysql://87.236.19.137:3306/tasotw9j_custom";
    private static final String user = "tasotw9j_custom";
    private static final String password = "J7Y#@@t*";
	private static final int ID=501234;
	private static Connection con;
    private static Statement stmt;
    private static ResultSet rs;
	public static boolean test()
	{
		try {
			String query = "select count(*) from customers where ID_CUSTOMER="+ID;
            // opening database connection to MySQL server
            con = (Connection) DriverManager.getConnection(url, user, password);
 
            // getting Statement object to execute query
            stmt = (Statement) con.createStatement();
 
            // executing SELECT query
            rs = stmt.executeQuery(query);
 
            while (rs.next()) {
                int count = rs.getInt(1);
                if(count>0){ 
                return true;                
                }                             
            } 
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close connection ,stmt and resultset here
            try { con.close(); } catch(SQLException se) { /*can't do anything */ }
            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }    
		return false;
	}
}
