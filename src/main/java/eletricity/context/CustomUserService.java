package eletricity.context;

import eletricity.manager.MemberManager;
import eletricity.model.Authority;
import eletricity.model.Member;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.openid.OpenIDAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Change
 * {@link UsernamePasswordAuthenticationToken UsernamePasswordAuthenticationToken}
 * to {@link OpenIDAuthenticationToken OpenIDAuthenticationToken} if intend to
 * use OpenId.
 * 如果要用OpenId，請將{@link UsernamePasswordAuthenticationToken UsernamePasswordAuthenticationToken}
 * 換成{@link OpenIDAuthenticationToken OpenIDAuthenticationToken}
 *
 * @author Kent Yeh
 */
@Service("customUserService")
public class CustomUserService implements UserDetailsService, AuthenticationUserDetailsService<UsernamePasswordAuthenticationToken> {

    private static final Logger logger = LogManager.getLogger(CustomUserService.class);
    @Autowired
    private MemberManager memberManager;

    /**
     * {@link DaoAuthenticationProvider DaoAuthenticationProvider} load
     * userDetails to compare user's password.<br>
     * {@link DaoAuthenticationProvider DaoAuthenticationProvider}叫用本函式取得用戶資料以比對密碼
     *
     * @param account
     * @return
     */
    @Override
    public UserDetails loadUserByUsername(String account) throws UsernameNotFoundException {
        //Find user data,找到用戶資料
        try {
            Member member = memberManager.findMemberByPrimaryKey(account);
            //Decide user's roles,自行決定如何給角色
            if(member==null){
                throw new UsernameNotFoundException("");
            }
            StringBuilder roles = null;
            for (Authority authority : member.getAuthorities()) {
                if (roles == null) {
                    roles = new StringBuilder(authority.getAuthority());
                } else {
                    roles.append(",").append(authority.getAuthority());
                }
            }
            if (roles == null) {
                return new CustomUserInfo(member, "");
            } else {
                return new CustomUserInfo(member, roles.toString());
            }
        }catch(UsernameNotFoundException ex){
            throw ex;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new UsernameNotFoundException(ex.getMessage(), ex);
        }
    }

    /**
     * Not used by
     * {@link DaoAuthenticationProvider DaoAuthenticationProvider}.<br>
     * {@link DaoAuthenticationProvider DaoAuthenticationProvider}目前用不到這個函式.
     * 3.1以後OpenId真正叫用這個函式
     *
     * @param token
     * @return
     */
    @Override
    public UserDetails loadUserDetails(UsernamePasswordAuthenticationToken token) throws UsernameNotFoundException {
        return loadUserByUsername(token.getName());
    }
}
