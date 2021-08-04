package de.uit.eai.sofia.hook;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.liferay.portal.kernel.dao.db.DBFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.EmailAddress;
import com.liferay.portal.model.Phone;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroupRole;
import com.liferay.portal.model.Website;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.RoleServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextThreadLocal;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.service.UserService;
import com.liferay.portal.service.UserServiceWrapper;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.announcements.model.AnnouncementsDelivery;

public class ExtUserService extends UserServiceWrapper {

    private static final String ERROR_USERS_ROLES_HOOK_LOG = "Error while inserting into USERS_ROLES_HOOK_LOG";
	private static final Logger logger = Logger.getLogger("ExtUserService");

	public ExtUserService(UserService userService) {
		super(userService);
	}

	@Override
    public void addRoleUsers(long roleId, long[] userIds) throws com.liferay.portal.kernel.exception.PortalException,
			com.liferay.portal.kernel.exception.SystemException {
		String curdate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

		long[] lUserIds = userIds;

        long liuid = getLoggedInUserId();
        User loggedInUser = UserLocalServiceUtil.getUser(liuid);

		for (long uid : lUserIds) {
			User updateUser = UserLocalServiceUtil.getUser(uid);
			long[] idcr = updateUser.getRoleIds();

			if (!ArrayUtil.contains(idcr, roleId)) {

				try {

                    String sql = "INSERT INTO USERS_ROLES_HOOK_LOG VALUES (" + updateUser.getUserId() + ", '"
							+ updateUser.getScreenName() + "', " + roleId + ", '"
							+ RoleLocalServiceUtil.getRole(roleId).getName() + "', '"
                            + loggedInUser.getScreenName() + "', " + loggedInUser.getUserId() + ",to_date('" + curdate
							+ "', 'dd/MM/YYYY hh24:mi:ss'), 'ADD', null)";

					DBFactoryUtil.getDB().runSQL(sql);
				} catch (IOException e) {
                    logger.log(Level.SEVERE, ERROR_USERS_ROLES_HOOK_LOG + e.toString());
				} catch (SQLException e) {
                    logger.log(Level.SEVERE, ERROR_USERS_ROLES_HOOK_LOG + e.toString());
				}
			}

		}
		super.addRoleUsers(roleId, userIds);
	}

    private static long getLoggedInUserId() throws PortalException, SystemException {
        ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();
        if (null == serviceContext) {
            logger.warning("ServiceContext is unavailable, returning default user");
            long companyId = PortalUtil.getDefaultCompanyId();
            long defaultUserId = UserLocalServiceUtil.getDefaultUserId(companyId);
            return defaultUserId;
        }

        return serviceContext.getUserId();
    }

    @Override
	public User updateUser(long userId, String oldPassword, String newPassword1, String newPassword2,
			boolean passwordReset, String reminderQueryQuestion, String reminderQueryAnswer, String screenName,
			String emailAddress, long facebookId, String openId, String languageId, String timeZoneId, String greeting,
			String comments, String firstName, String middleName, String lastName, int prefixId, int suffixId,
			boolean male, int birthdayMonth, int birthdayDay, int birthdayYear, String smsSn, String aimSn,
			String facebookSn, String icqSn, String jabberSn, String msnSn, String mySpaceSn, String skypeSn,
			String twitterSn, String ymSn, String jobTitle, long[] groupIds, long[] organizationIds, long[] roleIds,
			List<UserGroupRole> userGroupRoles, long[] userGroupIds, List<Address> addresses,
			List<EmailAddress> emailAddresses, List<Phone> phones, List<Website> websites,
			List<AnnouncementsDelivery> announcementsDelivers, ServiceContext serviceContext) throws PortalException,
			SystemException {

		User updateUser = super.getUserById(userId);
		long[] idsBefore = updateUser.getRoleIds();

		User postUser = super.updateUser(userId, oldPassword, newPassword1, newPassword2, passwordReset,
				reminderQueryQuestion, reminderQueryAnswer, screenName, emailAddress, facebookId, openId, languageId,
				timeZoneId, greeting, comments, firstName, middleName, lastName, prefixId, suffixId, male,
				birthdayMonth, birthdayDay, birthdayYear, smsSn, aimSn, facebookSn, icqSn, jabberSn, msnSn, mySpaceSn,
				skypeSn, twitterSn, ymSn, jobTitle, groupIds, organizationIds, roleIds, userGroupRoles, userGroupIds,
				addresses, emailAddresses, phones, websites, announcementsDelivers, serviceContext);

		long[] idsAfter = updateUser.getRoleIds();
		List<Long> listAfter = new Vector<Long>();
		List<Long> listBefore = new Vector<Long>();

		for (int i = 0; i < idsAfter.length; i++) {
			listAfter.add(idsAfter[i]);
		}

		for (int i = 0; i < idsBefore.length; i++) {
			listBefore.add(idsBefore[i]);
		}

		List<Long> listBeforeCopy = new Vector<Long>(listBefore);
		List<Long> listAfterCopy = new Vector<Long>(listAfter);

		listAfterCopy.removeAll(listBeforeCopy);

		User lin = super.getUserById(serviceContext.getUserId());

		String curdate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
		for (Long id : listAfterCopy) {
			Role role = RoleServiceUtil.getRole(id);
            String sql = "INSERT INTO USERS_ROLES_HOOK_LOG VALUES (" + postUser.getUserId() + ", '"
					+ postUser.getScreenName() + "', " + role.getRoleId() + ", '" + role.getName() + "', '"
					+ lin.getScreenName() + "', " + lin.getUserId() + ",to_date('" + curdate
					+ "', 'dd/MM/YYYY hh24:mi:ss'), 'ADD', null)";
			try {
				DBFactoryUtil.getDB().runSQL(sql);
			} catch (IOException e) {
                logger.log(Level.SEVERE, ERROR_USERS_ROLES_HOOK_LOG + e.toString());
			} catch (SQLException e) {
                logger.log(Level.SEVERE, ERROR_USERS_ROLES_HOOK_LOG + e.toString());
			}
		}

		listBeforeCopy = listBefore;
		listAfterCopy = listAfter;
		listBeforeCopy.removeAll(listAfterCopy);

		for (Long id : listBeforeCopy) {
			Role role = RoleServiceUtil.getRole(id);
			System.out.println("------------------------");
            String sql = "INSERT INTO USERS_ROLES_HOOK_LOG VALUES (" + postUser.getUserId() + ", '"
					+ postUser.getScreenName() + "', " + role.getRoleId() + ", '" + role.getName() + "', '"
					+ lin.getScreenName() + "', " + lin.getUserId() + ",to_date('" + curdate
					+ "', 'dd/MM/YYYY hh24:mi:ss'), 'REMOVE', null)";
			try {
				DBFactoryUtil.getDB().runSQL(sql);
			} catch (IOException e) {
                logger.log(Level.SEVERE, ERROR_USERS_ROLES_HOOK_LOG + e.toString());
			} catch (SQLException e) {
                logger.log(Level.SEVERE, ERROR_USERS_ROLES_HOOK_LOG + e.toString());
			}
		}

		return postUser;
	}

	@Override
    public void unsetRoleUsers(long roleId, long[] userIds) throws com.liferay.portal.kernel.exception.PortalException,
			com.liferay.portal.kernel.exception.SystemException {

		String curdate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

        long liuid = getLoggedInUserId();
        User loggedInUser = UserLocalServiceUtil.getUser(liuid);

		long[] lUserIds = userIds;
		for (long uid : lUserIds) {
			User updateUser = UserLocalServiceUtil.getUser(uid);
			long[] idcr = updateUser.getRoleIds();

			if (ArrayUtil.contains(idcr, roleId)) {

				try {

                    String sql = "INSERT INTO USERS_ROLES_HOOK_LOG VALUES (" + updateUser.getUserId() + ", '"
							+ updateUser.getScreenName() + "', " + roleId + ", '"
							+ RoleLocalServiceUtil.getRole(roleId).getName() + "', '"
                            + loggedInUser.getScreenName() + "', " + loggedInUser.getUserId() + ",to_date('" + curdate
							+ "', 'dd/MM/YYYY hh24:mi:ss'), 'REMOVE', null)";

					DBFactoryUtil.getDB().runSQL(sql);
				} catch (IOException e) {
                    logger.log(Level.SEVERE, ERROR_USERS_ROLES_HOOK_LOG + e.toString());
				} catch (SQLException e) {
                    logger.log(Level.SEVERE, ERROR_USERS_ROLES_HOOK_LOG + e.toString());
				}
			}
		}
		super.unsetRoleUsers(roleId, userIds);
	}

}