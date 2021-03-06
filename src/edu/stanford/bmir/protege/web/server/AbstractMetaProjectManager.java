package edu.stanford.bmir.protege.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.stanford.bmir.protege.web.client.rpc.data.UserData;
import edu.stanford.bmir.protege.web.client.ui.openid.constants.OpenIdConstants;
import edu.stanford.smi.protege.server.metaproject.MetaProject;
import edu.stanford.smi.protege.server.metaproject.Operation;
import edu.stanford.smi.protege.server.metaproject.Policy;
import edu.stanford.smi.protege.server.metaproject.ProjectInstance;
import edu.stanford.smi.protege.server.metaproject.PropertyValue;
import edu.stanford.smi.protege.server.metaproject.ServerInstance;
import edu.stanford.smi.protege.server.metaproject.User;

public abstract class AbstractMetaProjectManager implements MetaProjectManager {
	
	private final static String SIGN_IS_AS_OP = "SignInAs";

    public boolean hasValidCredentials(String userName, String password) {
        if (getMetaProject() == null) {
            return false;
        }
        User user = getMetaProject().getUser(userName);
        if (user == null) {
            return false;
        }
        return user.verifyPassword(password);
    }

    public void changePassword(String userName, String password) {
        final MetaProject metaProject = getMetaProject();
        if (metaProject == null) {
            throw new IllegalStateException("Metaproject is set to null");
        }
        User user = metaProject.getUser(userName);
        if (user == null) {
            throw new IllegalArgumentException("Invalid user name: " + userName);
        }
        user.setPassword(password);
    }

    public List<UserData> getUsers(String userName) {
    	 List<UserData> users = new ArrayList<UserData>();
         if (userName == null) {
             return users;
         }
         
         if (isServerOperationAllowed(userName, SIGN_IS_AS_OP) == false) {
        	 throw new IllegalStateException("User " + userName + " does not have the permission to"
        	 		+ " sign in as a different user.");
         }
         
         final MetaProject metaProject = getMetaProject();
      
         Set<User> userObjs = metaProject.getUsers();
         
         return getSortedUserList(userObjs);
    }
    
    
    private List<UserData> getSortedUserList(Set<User> userObjs) {
    	List<UserData> users = new ArrayList<UserData>();
    
    	for (User user : userObjs) {
			users.add(AuthenticationUtil.createUserData(user.getName()));
		}
    	
    	Collections.sort(users, new Comparator<UserData>() {

			@Override
			public int compare(UserData o1, UserData o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
    	
    	return users;
    }
    
    
    public String getUserEmail(String userName) {
        final MetaProject metaProject = getMetaProject();
        if (metaProject == null) {
            throw new IllegalStateException("Metaproject is set to null");
        }
        User user = metaProject.getUser(userName);
        if (user == null) {
            throw new IllegalArgumentException("Invalid user name: " + userName);
        }
        return user.getEmail();
    }

    public void setUserEmail(String userName, String email) {
        final MetaProject metaProject = getMetaProject();
        if (metaProject == null) {
            throw new IllegalStateException("Metaproject is set to null");
        }
        User user = metaProject.getUser(userName);
        if (user == null) {
            throw new IllegalArgumentException("Invalid user name: " + userName);
        }
        user.setEmail(email);
    }

    public Collection<Operation> getAllowedOperations(String projectName, String userName) {
        Collection<Operation> allowedOps = new ArrayList<Operation>();
        final MetaProject metaProject = getMetaProject();
        if (metaProject == null) {
            throw new IllegalStateException("Metaproject is set to null");
        }
        Policy policy = metaProject.getPolicy();
        User user = policy.getUserByName(userName);
        ProjectInstance project = getMetaProject().getPolicy().getProjectInstanceByName(projectName);
        if (user == null || project == null) {
            return allowedOps;
        }
        for (Operation op : policy.getKnownOperations()) {
            if (policy.isOperationAuthorized(user, op, project)) {
                allowedOps.add(op);
            }
        }
        return allowedOps;
    }

    public Collection<Operation> getDefinedOperations() {
        Collection<Operation> ops = new ArrayList<Operation>();
        final MetaProject metaProject = getMetaProject();
        if (metaProject == null) {
            throw new IllegalStateException("Metaproject is set to null");
        }
        return metaProject.getOperations();
    }

    public Collection<Operation> getAllowedServerOperations(String userName) {
        Collection<Operation> allowedOps = new ArrayList<Operation>();
        if (userName == null) {
            return allowedOps;
        }
        final MetaProject metaProject = getMetaProject();
        if (metaProject == null) {
            throw new IllegalStateException("Metaproject is set to null");
        }
        Policy policy = metaProject.getPolicy();
        User user = policy.getUserByName(userName);
        ServerInstance firstServerInstance = metaProject.getPolicy().getFirstServerInstance();
        if (user == null || firstServerInstance == null) {
            return allowedOps;
        }
        for (Operation op : policy.getKnownOperations()) {
            if (policy.isOperationAuthorized(user, op, firstServerInstance)) {
                allowedOps.add(op);
            }
        }
        return allowedOps;
    }

    @Override
    public boolean isServerOperationAllowed(String userName, String operation) {
     if (userName == null) {
         return false;
     }
     
     final MetaProject metaProject = getMetaProject();
     if (metaProject == null) {
         throw new IllegalStateException("Metaproject is set to null");
     }
     
     Operation op = metaProject.getOperation(operation);
     
     if (op == null) {
    	 return false;
     }
     
     Policy policy = metaProject.getPolicy();
     User user = policy.getUserByName(userName);
     ServerInstance firstServerInstance = metaProject.getPolicy().getFirstServerInstance();

     if (user == null || firstServerInstance == null) {
         return false;
     }
     
     return policy.isOperationAuthorized(user, op, firstServerInstance);
    }
    
    @Override
    public boolean isOperationAllowed(String projectName, String userName, String operation) {
        if (userName == null) {
            return false;
        }
        
        final MetaProject metaProject = getMetaProject();
        if (metaProject == null) {
            throw new IllegalStateException("Metaproject is set to null");
        }
        
        Operation op = metaProject.getOperation(operation);
        
        if (op == null) {
       	 return false;
        }
        
        Policy policy = metaProject.getPolicy();
        User user = policy.getUserByName(userName);
        ProjectInstance project = getMetaProject().getPolicy().getProjectInstanceByName(projectName);
       
        if (user == null || project == null) {
            return false;
        }
        
        return policy.isOperationAuthorized(user, op, project);
    }
    
    
    public UserData getUserAssociatedWithOpenId(String userOpenId) {
        UserData userData = null;

        if (userOpenId == null) {
            return null;
        }

        Set<User> users = getMetaProject().getUsers();

        boolean gotUser = false;
        for (Iterator<User> iterator = users.iterator(); iterator.hasNext();) {
            if (gotUser) {
                break;
            }

            User user = iterator.next();

            Collection<PropertyValue> propColl = user.getPropertyValues();

            for (Iterator<PropertyValue> iterator2 = propColl.iterator(); iterator2.hasNext();) {
                PropertyValue propertyValue = iterator2.next();
                if (propertyValue.getPropertyName().startsWith(OpenIdConstants.OPENID_PROPERTY_PREFIX)
                        && propertyValue.getPropertyName().endsWith(OpenIdConstants.OPENID_PROPERTY_URL_SUFFIX)) {

                    if (propertyValue.getPropertyValue().trim().equalsIgnoreCase(userOpenId)) {
                        userData = AuthenticationUtil.createUserData(user.getName());
                        gotUser = true;
                        break;
                    }

                }
            }
        }

        return userData;
    }


    public User getUser(String userNameOrEmail) {
        if (userNameOrEmail == null) {
            return null;
        }

        //try to get it by name first
        User user = getMetaProject().getUser(userNameOrEmail);
        if (user != null) {
            return user;
        }

        //get user by email
        Set<User> users = getMetaProject().getUsers();
        Iterator<User> it = users.iterator();

        while (it.hasNext() && user == null) {
            User u = it.next();
            if (userNameOrEmail.equals(u.getEmail())) {
                user = u;
            }
        }

        return user;
    }

}
