package hibernate.dao;

import hibernate.entity.EntryTime;
import hibernate.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class EntryTimeDao {

    public void addEntryTime(EntryTime entryTime) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            session.save(entryTime);

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

}
