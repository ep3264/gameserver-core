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
package custom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;



/**
 * @author user
 *
 */
public class PremiumAccount
{
	public static void addPremiumServices(L2PcInstance player, String AccName, int days)
	{
		if (player == null)
			return;

		Calendar finishtime = Calendar.getInstance();
		finishtime.set(Calendar.SECOND, 0);
		finishtime.add(Calendar.DAY_OF_MONTH, days);

		if(!player.getAccountName().equalsIgnoreCase(AccName))
		{
			player = null;
			for(L2PcInstance pc : L2World.getInstance().getAllPlayers())
				if(pc.getAccountName().equalsIgnoreCase(AccName))
				{
					player = pc;
					break;
				}

			if(player==null || player.getClient()==null)
			{
				Connection con  = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement stm = con.prepareStatement("REPLACE account_data VALUES (?,'premium',?)");
					stm.setString(1, AccName.toLowerCase());
					stm.setString(2, String.valueOf(finishtime.getTimeInMillis()));
					stm.execute();
					stm.close();
					con.close();
				}
				catch(SQLException e)
				{}
				return;
			}
		}

		player.setPremiumService(finishtime.getTimeInMillis());		
		player.getClient().storeData();
		
	}
}
