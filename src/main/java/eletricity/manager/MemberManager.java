package eletricity.manager;

import eletricity.context.ValidationUtils;
import eletricity.model.Authority;
import eletricity.model.Dao;
import eletricity.model.Member;
import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 *
 * @author Kent Yeh
 */
@Repository("memberManager")
@Transactional(readOnly = true)
public class MemberManager extends PropertyEditorSupport implements ApplicationContextAware {

    private static final Logger logger = LogManager.getLogger(MemberManager.class);

    private org.springframework.context.ApplicationContext context;

    @Autowired(required = false)
    @Qualifier("messageAccessor")
    MessageSourceAccessor messageAccessor;

    @Autowired
    ValidationUtils vu;

    @Override
    public void setApplicationContext(org.springframework.context.ApplicationContext ctx) throws BeansException {
        this.context = ctx;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            setValue(StringUtils.hasText(text) ? findMemberByPrimaryKey(text) : null);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        return getValue() == null ? "" : getValue().toString();
    }

    public org.springframework.context.ApplicationContext getContext() {
        return context;
    }

    public MessageSourceAccessor getMessageAccessor() {
        return messageAccessor;
    }

    protected Exception extractSQLException(Exception ex) {
        Throwable result = ex;
        boolean found = false;
        while (result != null) {
            if (result instanceof java.sql.SQLException) {
                found = true;
                break;
            } else if (result.getCause() == null) {
                break;
            } else {
                result = result.getCause();
            }
        }

        return found ? (java.sql.SQLException) result : ex;
    }

    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
    public Member findMemberByPrimaryKey(String account) throws Exception {
        Dao dao = context.getBean(Dao.class);
        Member member = dao.findMemberByPrimaryKey(account);
        if (member != null) {
            member.setAuthorities(dao.findAuthorityByAccount(account));
        }
        return member;
    }

    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
    public List<Member> findAvailableUsers() throws Exception {
        return context.getBean(Dao.class).findAvailableUsers();
    }

    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
    public List<Member> findAllUsers() throws Exception {
        return context.getBean(Dao.class).findAllUsers();
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void newMember(Member member) throws Exception {
        try {
            Dao dao = context.getBean(Dao.class);
            vu.validateMessage(member, RuntimeException.class);
            dao.newMember(member);
            List<Authority> authories = member.getAuthorities();
            if (authories != null) {
                for (Authority authority : authories) {
                    vu.validateMessage(authority, RuntimeException.class);
                    dao.newAuthority(authority);
                }
            }
        } catch (Exception ex) {
            logger.debug("{}{}", messageAccessor.getMessage("exception.newMember"), ex.getMessage());
            throw new RuntimeException(ex.getMessage(), extractSQLException(ex));
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void updateMember(Member member) throws Exception {
        Dao dao = context.getBean(Dao.class);
        vu.validateMessage(member, RuntimeException.class);
        dao.updateMember(member);
        List<Authority> authories = member.getAuthorities();
        if (authories != null && !authories.isEmpty()) {
            List<String> auths = new ArrayList<>();
            for (Authority authority : authories) {
                auths.add(authority.getAuthority());
                vu.validateMessage(authority, RuntimeException.class);
                if (dao.findAuthorityByBean(authority) == null) {
                    logger.debug("insert authority id = {}", dao.newAuthority(authority));
                }
            }
            logger.debug("Authorities remove count = {}", dao.removeAuthories(member.getAccount(), auths));
        } else {
            dao.removeAuthories(member.getAccount());
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public int updatePass(String account, String oldPass, String newPass) throws Exception {
        return context.getBean(Dao.class).changePasswd(account, oldPass, newPass);
    }

}
